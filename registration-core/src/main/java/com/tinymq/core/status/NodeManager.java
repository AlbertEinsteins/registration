package com.tinymq.core.status;

import com.tinymq.common.protocol.RequestCode;
import com.tinymq.core.ConsensusService;
import com.tinymq.core.RegistrationConfig;
import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.dto.AppendEntriesResponse;
import com.tinymq.core.dto.VoteRequest;
import com.tinymq.core.dto.VoteResponse;
import com.tinymq.core.dto.outer.StateModel;
import com.tinymq.core.exception.AppendLogException;
import com.tinymq.core.exception.RegistrationVoteException;
import com.tinymq.core.processor.AcceptClientProcessor;
import com.tinymq.core.processor.AppendEntriesProcessor;
import com.tinymq.core.processor.RequestVoteProcessor;
import com.tinymq.core.store.CommitLogEntry;
import com.tinymq.core.store.StoreManager;
import com.tinymq.remote.exception.RemotingConnectException;
import com.tinymq.remote.exception.RemotingSendRequestException;
import com.tinymq.remote.exception.RemotingTimeoutException;
import com.tinymq.remote.exception.RemotingTooMuchException;
import com.tinymq.remote.netty.*;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 管理节点的状态，完成两件事
 * 1。选举
 * 2。数据同步
 */
public class NodeManager {
    private static final Logger LOG = LoggerFactory.getLogger(NodeManager.class);

    private final RegistrationConfig registrationConfig;
    private final NodeStatus nodeStatus = new NodeStatus();

    /*一段时间没收到Heartbeat就发送vote请求*/
    /* 定时器线程 */
    private final RandomResettableTimer randomResettableTimer;

    // 每个节点选择成为主节点的时间范围
    private final int[] electionIntervalTimeoutMillis = new int[]{1000, 2000};
    // 转发超时事件
    private final long redirectTimeoutMillis = 1000;

    /*定时器发送心跳线程*/
    private final ScheduledExecutorService singleScheduleExecutor;
    /*执行内部的rpc请求*/
    private final ExecutorService publicThreadPool;

    /* 发起异步请求辅助 */
    private final ExecutorService asideThreadPool;

    //========================= 通信============================
    private NettyClientConfig nettyClientConfig;
    private final NettyRemotingClient nettyRemotingClient;
    private NettyServerConfig nettyServerConfig;
    private final NettyRemotingServer nettyRemotingServer;

    private AppendEntriesProcessor appendEntriesProcessor;
    private RequestVoteProcessor requestVoteProcessor;

    private AcceptClientProcessor acceptClientProcessor;
    //============================================================

    /*在启动时，由外部注入的节点列表*/
    private volatile Set<String> addrList = new HashSet<>();
    private final String selfAddr;
    private ConsensusService consensusService;


    //============= store module =======================
    private final StoreManager storeManager;


    public NodeManager(final RegistrationConfig registrationConfig, final StoreManager storeManager) {
        this.registrationConfig = registrationConfig;
        this.storeManager = storeManager;

        //========伪造读入节点=============
        List<String> addrs = resolveConfigFile("test");
        readServerNodes(addrs);
        //================================

        this.nettyClientConfig = new NettyClientConfig();
        this.nettyRemotingClient = new NettyRemotingClient(nettyClientConfig);
        this.nettyServerConfig = new NettyServerConfig();
        this.nettyServerConfig.setListenPort(registrationConfig.getListenPort());
        this.nettyRemotingServer = new NettyRemotingServer(nettyServerConfig);
        this.selfAddr = "127.0.0.1:" + registrationConfig.getListenPort();

        this.registerProcessors();
        this.createService();

        this.randomResettableTimer = new RandomResettableTimer(new ElectionTask(), 5000,
                electionIntervalTimeoutMillis[0], electionIntervalTimeoutMillis[1]);
        this.publicThreadPool = Executors.newWorkStealingPool();
        this.singleScheduleExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "[ScheduledThreadPool-heartbeat]");
            }
        });
        this.asideThreadPool = Executors.newWorkStealingPool();

        // 状态初始化为follower
        this.setNodeStatus(NodeStatus.STATUS.FOLLOWER);
    }

    public void start() {
        //TODO: ...
        this.nettyRemotingServer.start();
        this.nettyRemotingClient.start();

        // 开启定时选举
        this.randomResettableTimer.start();
    }

    public void shutdown() {
        this.nettyRemotingServer.shutdown();
        this.nettyRemotingClient.shutdown();

        this.randomResettableTimer.shutdown();
        this.singleScheduleExecutor.shutdown();
        this.publicThreadPool.shutdown();
        this.asideThreadPool.shutdown();
    }

    public void resetElectionTimer() {
        LOG.info("reset timer");
        this.randomResettableTimer.resetTimer();
    }


    //============= mock method
    private List<String> resolveConfigFile(String filePath) {
        List<String> addrs = new ArrayList<>();
        addrs.add("127.0.0.1:7800");
        addrs.add("127.0.0.1:7801");
        return addrs;
    }
    //=============

    private void createService() {
        this.consensusService = new ConsensusServiceImpl();
    }

    private void registerProcessors() {
        this.appendEntriesProcessor = new AppendEntriesProcessor(this, storeManager);
        this.requestVoteProcessor = new RequestVoteProcessor(this);

        nettyRemotingServer.registerProcessor(RequestCode.APPENDENTRIES_EMPTY, appendEntriesProcessor, null);
        nettyRemotingServer.registerProcessor(RequestCode.REIGISTRATION_REQUESTVOTE, requestVoteProcessor, null);
    }

    /*urls <ip:port>;...*/
    public void readServerNodes(List<String> urls) {
        LOG.info("[NodeManager] read server url nodes {}", urls);
        synchronized (this) {
            this.addrList.addAll(urls);
        }
    }

    public void readServerNode(String url) {
        synchronized (this) {
            this.addrList.add(url);
        }
    }

    public RemotingCommand redirectToLeader(RemotingCommand req) {
        if(!nodeStatus.getStatus().equals(NodeStatus.STATUS.FOLLOWER)) {
            LOG.info("the current node {} is not in state [follower], cancel redirect", selfAddr);
            return null;
        }

        try {
            return this.nettyRemotingClient.invokeSync(nodeStatus.getLeaderAddr(),
                    req, this.redirectTimeoutMillis);
        } catch (RemotingConnectException | RemotingSendRequestException | RemotingTimeoutException |
                 InterruptedException e) {
            LOG.error("the node {} in state {}, redirect to leader {} error",
                    selfAddr, NodeStatus.STATUS.FOLLOWER, nodeStatus.getLeaderAddr(), e);
            return RemotingCommand.createResponse(req.getCode(), "error occurred when redirect to leader");
        }
    }


    public void handleRequest(final StateModel stateModel) throws AppendLogException {
        //todo: store locally, then replicate it to the other follower node
        final int term = nodeStatus.getCurTerm().get();
        final int prevLastIndex = storeManager.getLogQueue().size() - 1;
        final int prevLastTerm;
        final int leaderCommitIdx = storeManager.getCommittedIndex();

        if(prevLastIndex == -1) {
            // first time
            prevLastTerm = term;
        } else {
            prevLastTerm = storeManager.getLogQueue().at(prevLastIndex).getTerm();
        }

        AppendEntriesRequest appendEntriesRequest =
                AppendEntriesRequest.create(term, selfAddr, prevLastIndex, prevLastTerm, leaderCommitIdx);
        CommitLogEntry entry = CommitLogEntry.create(term, JSONSerializer.encode(stateModel));
        appendEntriesRequest.setCommitLogEntries(Collections.singletonList(entry));


        this.storeManager.appendLog(appendEntriesRequest);
        // 同步日志, 不单独送，借助heartbeat
        new SyncLogTask(3, appendEntriesRequest).run();
    }


    public int getCurTerm() {
        return this.nodeStatus.getCurTerm().get();
    }
    public void setCurTerm(int term) {
        this.nodeStatus.getCurTerm().set(term);
    }

    public void setNodeStatus(NodeStatus.STATUS status) {
        this.nodeStatus.setStatus(status);
    }

    public NodeStatus.STATUS getNodeStatus() {
        return nodeStatus.getStatus();
    }

    public void setNettyClientConfig(NettyClientConfig nettyClientConfig) {
        this.nettyClientConfig = nettyClientConfig;
    }

    public void setNettyServerConfig(NettyServerConfig nettyServerConfig) {
        this.nettyServerConfig = nettyServerConfig;
    }

    public String getSelfAddr() {
        return selfAddr;
    }

    public void setLeader(String leaderAddr) {
        this.nodeStatus.setLeaderAddr(leaderAddr);
    }


    class SyncLogTask implements Runnable {
        private int retry = 3;
        private final AppendEntriesRequest appendEntriesRequest;

        /*当该entry is committed 时，执行到entry*/
        private int copySuccessCnt = 0;
        private final ReentrantLock lockNodes = new ReentrantLock();
        private final ArrayList<String> nodes = new ArrayList<>();

        public SyncLogTask(int retry, final AppendEntriesRequest appendEntriesRequest) {
            this.retry = retry;
            this.appendEntriesRequest = appendEntriesRequest;
        }
        @Override
        public void run() {
            final ConsensusService consensusService = NodeManager.this.consensusService;
            nodes.addAll(NodeManager.this.addrList);
            copySuccessCnt ++;

            Iterator<String> iter = nodes.iterator();
            while(iter.hasNext()) {
                String addr = iter.next();
                publicThreadPool.submit(() -> {
                    try {
                        AppendEntriesResponse response = consensusService.copyCommitLog(addr, appendEntriesRequest);

                        lockNodes.lock();
                        if (response != null && response.isSuccess()) {
                            iter.remove();
                            copySuccessCnt++;
                        }
                    } catch (Exception e) {
                        LOG.warn("error occurred when replicate the log to follower node", e);
                    } finally {
                        lockNodes.unlock();
                    }
                });
            }

            if(copySuccessCnt > addrList.size() / 2) {
                // this log is replicated by a majority of the node, make it committed.
                LOG.info("the log is replicated by a majority of the node, make it committed.");
                NodeManager.this.storeManager.increCommitIndex();
            }

            //TODO: copy infinitely if any node failed

        }
    }
    class ElectionTask implements Runnable {
        private final AtomicInteger voteCount = new AtomicInteger(0);
        private final CountDownLatch latch = new CountDownLatch(addrList.size() - 1);

        @Override
        public void run() {
            LOG.debug("async election start...");

            final ConsensusService consensusService = NodeManager.this.consensusService;
            final ExecutorService publicThreadPool = NodeManager.this.publicThreadPool;

            final int newTerm = NodeManager.this.nodeStatus.incrementTerm();

            nodeStatus.setStatus(NodeStatus.STATUS.CANDIDATE);
            voteCount.incrementAndGet();
            VoteRequest voteRequest = VoteRequest.createVote(newTerm, selfAddr, -1, -1);
            for (String addr :
                    addrList) {
                if (selfAddr.equals(addr)) {
                    continue;
                }
                publicThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            VoteResponse voteResponse = consensusService.invokeVote(addr, voteRequest, 500);

                            LOG.info("node {} receive remote node {} vote response {}", selfAddr, addr, voteResponse);
                            if (voteResponse.isVoteGranted()) {
                                voteCount.incrementAndGet();
                            }

                        } catch (RegistrationVoteException voteException) {
                            LOG.warn("the remote server {} has no response", voteException.getRemoteAddr());
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            // election ok
            try {
                latch.await();
            } catch (InterruptedException e) {
                LOG.error("the server {} wait for votes error, the latch wait interruptedly", selfAddr);
            }

            LOG.info("node {} receive {} notes", selfAddr, voteCount.get());
            if(nodeStatus.getStatus().equals(NodeStatus.STATUS.FOLLOWER)) {
                // 已经投了其他人，本次投票不算数
                return ;
            }
            if (voteCount.get() > addrList.size() / 2) {
                LOG.info("the node {} receive {} votes, then turn to the leader", selfAddr, voteCount.get());
                NodeManager.this.setNodeStatus(NodeStatus.STATUS.LEADER);
                // 不再进行定时选举
                NodeManager.this.randomResettableTimer.clearTimer();

                // 启动心跳, 重置其他节点的任务
                NodeManager.this.singleScheduleExecutor.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        new HeartBeatTask().run();
                    }
                }, 0, registrationConfig.getHeartBeatTimeMillis(), TimeUnit.MILLISECONDS);

                LOG.debug("end election...");
                return;
            }

            LOG.info("the node {} receive {} notes, but can not ahead the majority, do nothing", selfAddr, voteCount.get());
            LOG.info("end election...");
        }

    }

    class  ConsensusServiceImpl implements ConsensusService {
        @Override
        public VoteResponse invokeVote(final String addr, final VoteRequest voteRequest, long timeoutMillis) throws RegistrationVoteException {
            final NettyRemotingClient rpcClient = NodeManager.this.nettyRemotingClient;

            RemotingCommand req = RemotingCommand.createRequest(RequestCode.REIGISTRATION_REQUESTVOTE);
            req.setBody(
                    JSONSerializer.encode(voteRequest)
            );

            try {
                RemotingCommand response = rpcClient.invokeSync(addr, req, timeoutMillis);
                if (response == null) {
                    LOG.warn("the remote server {} maybe has a problem", addr);
                    throw new RegistrationVoteException("the remote node {} response nothing", addr);
                }
                return JSONSerializer.decode(response.getBody(), VoteResponse.class);
            } catch (InterruptedException e) {
                LOG.error("an error occurred in election, check the remote addr {}", addr);
            } catch (RemotingConnectException | RemotingSendRequestException | RemotingTimeoutException ex) {
                LOG.error("invoke error send vote to {} request exception", addr);
            }
            return null;
        }

        @Override
        public AppendEntriesResponse copyCommitLog(String addr, AppendEntriesRequest appendEntriesRequest) {
            final NettyRemotingClient rpcClient = NodeManager.this.nettyRemotingClient;

            RemotingCommand copyLog = RemotingCommand.createRequest(RequestCode.APPENDENTRIES);
            copyLog.setBody(
                    JSONSerializer.encode(appendEntriesRequest)
            );


            return null;
        }
    }


    class HeartBeatTask implements Runnable {

        @Override
        public void run() {
            final ExecutorService publicThreadPool = NodeManager.this.publicThreadPool;
            final NettyRemotingClient rpcClient = NodeManager.this.nettyRemotingClient;
            final Set<String> addrs = NodeManager.this.addrList;
            final int curTerm = nodeStatus.getCurTerm().get();

            LOG.info("heartbeat start to send...");
            RemotingCommand heartbeat = RemotingCommand.createRequest(RequestCode.APPENDENTRIES_EMPTY);
            heartbeat.setBody(
                    JSONSerializer.encode(AppendEntriesRequest.createEmpty(curTerm, selfAddr))
            );
            for (String addr :
                    addrs) {
                if (addr.equals(selfAddr)) {
                    continue;
                }
                publicThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            rpcClient.invokeOneway(addr, heartbeat, registrationConfig.getHeartBeatTimeMillis());
                        } catch (InterruptedException e) {
                            LOG.error("an error occurred in election when send heartbeat, check the remote addr {}", addr);
                        } catch (RemotingConnectException | RemotingSendRequestException | RemotingTimeoutException ex) {
                            LOG.error("invoke error send vote to {} request exception", addr);
                        } catch (RemotingTooMuchException e) {
                            LOG.error("send heart beat too fast..., remote addr, {}, self.addr {}", addr, selfAddr);
                        }
                    }
                });

            }
            LOG.info("heartbeat send ending...");

        }
    }
}
