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
import com.tinymq.core.exception.ReplicatedLogException;
import com.tinymq.core.processor.AcceptClientProcessor;
import com.tinymq.core.processor.AppendEntriesProcessor;
import com.tinymq.core.processor.RequestVoteProcessor;
import com.tinymq.core.store.CommitLogEntry;
import com.tinymq.core.store.StoreManager;
import com.tinymq.remote.exception.RemotingConnectException;
import com.tinymq.remote.exception.RemotingSendRequestException;
import com.tinymq.remote.exception.RemotingTimeoutException;
import com.tinymq.remote.netty.*;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ScheduledExecutorService heartBeatTimer;
    private final HeartBeatTask heartBeatTask;

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


    //========== state machine ====================
    private final StateMachine stateMachine;

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
        this.selfAddr = "127.0.0.1" + ":" + registrationConfig.getListenPort();

        this.stateMachine = new KVStateMachine();

        this.registerProcessors();
        this.createService();

        this.randomResettableTimer = new RandomResettableTimer(new ElectionTask(), 5000,
                electionIntervalTimeoutMillis[0], electionIntervalTimeoutMillis[1]);
        this.publicThreadPool = Executors.newWorkStealingPool();
        this.asideThreadPool = Executors.newWorkStealingPool();

        this.heartBeatTimer = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "[ScheduledThreadPool-heartbeat]");
            }
        });
        this.heartBeatTask = new HeartBeatTask();

        // 状态初始化为follower
        this.setNodeStatus(NodeStatus.STATUS.FOLLOWER);
    }

    private void initNodeStatus() {
        this.nodeStatus.initPeerNodeStatus(addrList);
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
        this.heartBeatTimer.shutdown();
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

        nettyRemotingServer.registerProcessor(RequestCode.APPENDENTRIES, appendEntriesProcessor, null);
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


    public RemotingCommand handleRequest(final StateModel stateModel) throws ReplicatedLogException {
        //todo: store locally, then replicate it to the other follower node
        final int curTerm = nodeStatus.getCurTerm().get();
        final CountDownLatch latch = new CountDownLatch(addrList.size() - 1);
        final ConsensusService consensusService = this.consensusService;
        AtomicInteger cpSuccessCnt = new AtomicInteger(0);

        CommitLogEntry entry = CommitLogEntry.create(curTerm,
                JSONSerializer.encode(stateModel));
        // leader store
        try {
            this.storeManager.appendLog(entry);
            cpSuccessCnt.incrementAndGet();
        } catch (AppendLogException e) {
            throw new ReplicatedLogException("method [handleRequest] storeLog locally exception");
        }

        // parallel replicate
        int lastLogIndex = storeManager.getLogQueue().size() - 1;
        for(String addr: addrList) {
            if (selfAddr.equals(addr)) {
                continue ;
            }
            publicThreadPool.submit(() -> {
                try {

                    AppendEntriesRequest appendEntriesRequest = prepareEmptyRequest(lastLogIndex);
                    CommitLogEntry entryByNodeAddr = prepareEntryByAddr(addr);
                    appendEntriesRequest.setCommitLogEntries(Collections.singletonList(entryByNodeAddr));

                    AppendEntriesResponse response = consensusService.replicateLog(addr, appendEntriesRequest,
                            registrationConfig.getHeartBeatTimeMillis(), false);

                    if(response == null) {
                        LOG.error("in method [handleRequest] when replicate log the remote {} response null", addr);
                        return ;
                    }

                    if(response.isSuccess()) {
                        cpSuccessCnt.incrementAndGet();
                    } else {
                        LOG.warn("in method [handleRequest] the remote node {} response failure when replicate log", addr);
                    }
                    NodeManager.this.processReplicateResponse(addr, response);
                } catch (ReplicatedLogException e) {
                    LOG.error("error occurred when replicate to remote node {}", addr, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.error("error occurred when wait for parrallel copy log", e);
            throw new ReplicatedLogException("error occurred when wait for parallel copy log");
        }

        // a majority of node replicate successfully
        if(cpSuccessCnt.get() > addrList.size() / 2) {
            //todo: commit, return success
            storeManager.increCommitIndex();
        }
        //todo: return false

    }

    private void processReplicateResponse(final String remoteAddr, AppendEntriesResponse response) {
        if(response.isSuccess()) {
            // 更新对应node的matchIndex
            NodeStatus.InnerPeerNodeStatus remoteNodeStatus = nodeStatus.getNodeStatus(remoteAddr);

            // set nextIndex and then increment nextindex
            nodeStatus.updatePeerNodeStatus(remoteAddr,
                    remoteNodeStatus.nextIndex + 1, remoteNodeStatus.nextIndex);
        }
    }

    //prepare Entry to replicate
    private CommitLogEntry prepareEntryByAddr(String addr) {
        CommitLogEntry entry = null;
        try {
            NodeStatus.InnerPeerNodeStatus status = nodeStatus.getNodeStatus(addr);
            int nextIdx = status.nextIndex;
            entry = storeManager.getByIndex(nextIdx);
        } catch (Exception e) {
            LOG.error("method [prepareReplicateRequest] exception occurred", e);
        }
        return entry;
    }
    // prepare the empty heartbeat package
    // prevLastIndex 是当前存储下标的前一个位置
    private AppendEntriesRequest prepareEmptyRequest(int readyForReplicateIndex) {
        int term = nodeStatus.getCurTerm().get();
        int leaderCommitIdx = storeManager.getCommittedIndex();
        int prevLastIndex = 0;
        int prevLastTerm = 0;

        prevLastIndex = readyForReplicateIndex - 1;

        if(prevLastIndex == -1) {
            // first time
            prevLastTerm = -1;
        } else {
            prevLastTerm = storeManager.getLogQueue().at(prevLastIndex).getTerm();
        }

        return AppendEntriesRequest.create(term, selfAddr, prevLastIndex, prevLastTerm, leaderCommitIdx);
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


    class ElectionTask implements Runnable {
        private final AtomicInteger voteCount = new AtomicInteger(0);
        private final CountDownLatch latch = new CountDownLatch(addrList.size() - 1);

        @Override
        public void run() {
            LOG.debug("async election start...");

            final ConsensusService consensusService = NodeManager.this.consensusService;
            final ExecutorService publicThreadPool = NodeManager.this.publicThreadPool;
            final int newTerm = NodeManager.this.nodeStatus.incrementTerm();
            int lastLogIndex = -1;
            int lastLogTerm = -1;

            nodeStatus.setStatus(NodeStatus.STATUS.CANDIDATE);
            voteCount.incrementAndGet();

            if(storeManager.getLogQueue().size() > 0) {
                lastLogIndex = storeManager.getLogQueue().size() - 1;
                lastLogTerm = storeManager.getLogQueue().at(lastLogIndex).getTerm();
            }

            VoteRequest voteRequest = VoteRequest.createVote(newTerm, selfAddr, lastLogIndex, lastLogTerm);

            for (String addr :
                    addrList) {
                if (selfAddr.equals(addr)) {
                    continue;
                }
                publicThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            VoteResponse voteResponse = consensusService.invokeVote(addr, voteRequest, 1000);

                            LOG.info("node {} receive remote node {} vote response {}", selfAddr, addr, voteResponse);
                            if (voteResponse.isVoteGranted()) {
                                voteCount.incrementAndGet();
                            }

                        } catch (RegistrationVoteException voteException) {
                            LOG.warn("the remote server {} has no response", voteException.getRemoteAddr(), voteException);
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

                //======= init node status in every election =======
                NodeManager.this.initNodeStatus();

                // 启动心跳, 重置其他节点的任务
                NodeManager.this.heartBeatTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        NodeManager.this.heartBeatTask.run();
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
        public AppendEntriesResponse replicateLog(String addr, AppendEntriesRequest appendEntriesRequest,
                                                  long timeoutMillis, boolean isHeartbeat) throws ReplicatedLogException {
            final NettyRemotingClient rpcClient = NodeManager.this.nettyRemotingClient;

            RemotingCommand req = null;
            if(isHeartbeat) {
                req = RemotingCommand.createRequest(RequestCode.APPENDENTRIES_EMPTY);
            } else {
                req = RemotingCommand.createRequest(RequestCode.APPENDENTRIES);
            }
            req.setBody(
                    JSONSerializer.encode(appendEntriesRequest)
            );

            try {
                RemotingCommand response = rpcClient.invokeSync(addr, req, timeoutMillis);
                if (response == null) {
                    LOG.error("the remote server {} maybe has a problem", addr);
                    throw new ReplicatedLogException(String.format("the remote node {%s} response nothing", addr));
                }
                return JSONSerializer.decode(response.getBody(), AppendEntriesResponse.class);
            } catch (InterruptedException e) {
                LOG.error("an error occurred in election, check the remote addr {}", addr, e);
            } catch (RemotingConnectException | RemotingSendRequestException | RemotingTimeoutException ex) {
                LOG.error("invoke error send vote to {} request exception", addr);
            }
            return null;
        }
    }


    class HeartBeatTask implements Runnable {

        @Override
        public void run() {
            final ExecutorService publicThreadPool = NodeManager.this.publicThreadPool;
            final Set<String> addrs = NodeManager.this.addrList;
            final int curTerm = nodeStatus.getCurTerm().get();

            //todo:  carry on the appendentries request if not null
            LOG.info("heartbeat start to send...");

            int logSize = storeManager.getLogQueue().size();
            for (String addr :
                    addrs) {
                if (addr.equals(selfAddr)) {
                    continue;
                }
                publicThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final AppendEntriesRequest heartbeat = prepareEmptyRequest(logSize);

                            AppendEntriesResponse response = consensusService.replicateLog(addr, heartbeat,
                                    registrationConfig.getHeartBeatTimeMillis(), true);
                            if(response == null) {
                                LOG.error("replicate log to remote node {} error occurred, the remote response null", addr);
                                return ;
                            }

                            NodeManager.this.processReplicateResponse(addr, response);
                        } catch (ReplicatedLogException e) {
                            LOG.error("replicate log to remote node {} error occurred", addr, e);
                        }
                    }
                });

            }
            LOG.info("heartbeat send ending...");

        }
    }

}
