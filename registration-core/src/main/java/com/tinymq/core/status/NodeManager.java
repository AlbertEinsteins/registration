package com.tinymq.core.status;

import com.tinymq.common.protocol.RequestCode;
import com.tinymq.core.config.RegistrationConfig;
import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.dto.AppendEntriesResponse;
import com.tinymq.core.dto.VoteRequest;
import com.tinymq.core.dto.VoteResponse;
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


import java.nio.charset.StandardCharsets;
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
    private final NodeStatus nodeStatus;

    /*一段时间没收到Heartbeat就发送vote请求*/
    /* 定时器线程 */
    private final RandomResettableTimer randomResettableTimer;

    // 每个节点选择成为主节点的时间范围
    private final int[] electionIntervalTimeoutMillis = new int[]{1500, 3000};

    /*定时器发送心跳线程*/
    private final ScheduledThreadPoolExecutor heartBeatTimer;
    private final HeartBeatTask heartBeatTask;

    /*执行内部的rpc请求*/
    private final ExecutorService publicThreadPool;

    /* 处理心跳请求 */
    private final ExecutorService apendEntryThreadPool;

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
    private StoreManager storeManager;


    //========== state machine ====================
    private StateMachine stateMachine;


    public NodeManager(final RegistrationConfig registrationConfig,
                       final StoreManager storeManager, final StateMachine stateMachine) {
        this.registrationConfig = registrationConfig;
        this.storeManager = storeManager;
        this.nodeStatus = new NodeStatus();
        this.stateMachine = stateMachine;

        this.addrList = registrationConfig.getAddrNodes();

        this.nettyClientConfig = new NettyClientConfig();
        this.nettyRemotingClient = new NettyRemotingClient(nettyClientConfig);
        this.nettyServerConfig = new NettyServerConfig();
        this.nettyServerConfig.setListenPort(registrationConfig.getListenPort());
        this.nettyRemotingServer = new NettyRemotingServer(nettyServerConfig);
        this.selfAddr = registrationConfig.getSelfAddr();


        this.registerProcessors();
        this.createService();

        this.randomResettableTimer = new RandomResettableTimer(new ElectionTask(), 3000,
                electionIntervalTimeoutMillis[0], electionIntervalTimeoutMillis[1]);
        this.publicThreadPool = Executors.newFixedThreadPool(registrationConfig.getPublicThreadPoolSize(), new ThreadFactory() {
            private final AtomicInteger threadIdx = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "[RegistrationThreadPool-" + threadIdx.getAndIncrement() + "]");
            }
        });

        this.apendEntryThreadPool = Executors.newFixedThreadPool(registrationConfig.getPublicThreadPoolSize(), new ThreadFactory() {
            private final AtomicInteger threadIdx = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "AppendEntryThreadPool-" + threadIdx.getAndIncrement() + "]");
            }
        });

        this.heartBeatTimer = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            private final AtomicInteger threadIdx = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("[NodeManager-Heartbeat-Scheduler-%d]", threadIdx.getAndIncrement()));
            }
        });
        this.heartBeatTask = new HeartBeatTask();

        // 状态初始化为follower
        this.setNodeStatus(NodeStatus.STATUS.FOLLOWER);
    }

    private void initNodeStatus(int leaderIndex) {
        this.nodeStatus.initPeerNodeStatus(addrList, leaderIndex);
    }

    public void start() {
        this.nettyRemotingServer.start();
        this.nettyRemotingClient.start();

        // 开启定时选举
        this.randomResettableTimer.start();
    }

    public void shutdown() {
        this.nettyRemotingServer.shutdown();
        this.nettyRemotingClient.shutdown();

        // 关闭线程池
        this.randomResettableTimer.shutdown();
        try {
            this.heartBeatTimer.shutdown();
            this.publicThreadPool.shutdown();
        } catch (Exception e) {
            LOG.info("exception occurred when shutdown...", e);
        }
    }

    public void resetElectionTimer() {
        LOG.debug("reset timer");
        this.randomResettableTimer.resetTimer();
    }


    private void createService() {
        this.consensusService = new ConsensusServiceImpl();
    }

    private void registerProcessors() {
        this.appendEntriesProcessor = new AppendEntriesProcessor(this, storeManager);
        this.requestVoteProcessor = new RequestVoteProcessor(this);
        this.acceptClientProcessor = new AcceptClientProcessor(this, stateMachine);

        nettyRemotingServer.registerProcessor(RequestCode.APPENDENTRIES, appendEntriesProcessor, null);
        nettyRemotingServer.registerProcessor(RequestCode.APPENDENTRIES_EMPTY, appendEntriesProcessor, null);
        nettyRemotingServer.registerProcessor(RequestCode.REIGISTRATION_REQUESTVOTE, requestVoteProcessor, null);

        // client get/put
        nettyRemotingServer.registerProcessor(RequestCode.REGISTRATION_CLIENT_WRITE, acceptClientProcessor, null);
        nettyRemotingServer.registerProcessor(RequestCode.REGISTRATION_CLIENT_READ, acceptClientProcessor, null);

        // watcher request processor
        nettyRemotingServer.registerProcessor(RequestCode.REGISTRATION_CLIENT_WATCHER_ADD, acceptClientProcessor, null);
        nettyRemotingServer.registerProcessor(RequestCode.REGISTRATION_CLIENT_WATCHER_DEL, acceptClientProcessor, null);
        nettyRemotingServer.registerProcessor(RequestCode.REGISTRATION_CLIENT_KEY_CREATE, acceptClientProcessor, null);
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
    public RemotingCommand handleReadRequest(final RemotingCommand request) {

        String key = new String(request.getBody(), StandardCharsets.UTF_8);
        String val = stateMachine.getByKey(key);

        RemotingCommand resp = RemotingCommand.createResponse(request.getCode(), "success");
        resp.setBody(
                JSONSerializer.encode(val)
        );
        return resp;
    }

    public RemotingCommand handleWriteRequest(final RemotingCommand request) throws ReplicatedLogException {
        final int curTerm = nodeStatus.getCurTerm().get();
        final CountDownLatch latch = new CountDownLatch(addrList.size() - 1);
        final ConsensusService consensusService = this.consensusService;
        AtomicInteger cpSuccessCnt = new AtomicInteger(0);

        final CommitLogEntry entry = CommitLogEntry.create(curTerm, request.getBody());
        // leader store
        int saveIndex = -1;
        try {
            saveIndex = this.storeManager.appendLog(entry);
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
            apendEntryThreadPool.submit(() -> {
                try {
                    AppendEntriesRequest appendEntriesRequest = prepareEmptyRequest(lastLogIndex);
                    appendEntriesRequest.setCommitLogEntries(Collections.singletonList(entry));
                    AppendEntriesResponse response = consensusService.replicateLog(addr, appendEntriesRequest,
                            registrationConfig.getHeartBeatTimeMillis(), false);

                    if(response == null) {
                        LOG.warn("the remote node {} can not reach when replicating log", addr);
                        return ;
                    }

                    if(response.isSuccess()) {
                        cpSuccessCnt.incrementAndGet();
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
            LOG.error("error occurred when in CountDownLatch waiting for parallel copy log", e);
            throw new ReplicatedLogException("error occurred when wait for paralleling copy log");
        }

        // a majority of node replicate successfully
        if(cpSuccessCnt.get() > addrList.size() / 2) {
            storeManager.setCommitIndexAndExec(saveIndex);
            return RemotingCommand.createResponse(request.getCode(), "save good");
        }
        storeManager.getLogQueue().pollLast();
        throw new ReplicatedLogException("there is not a majority of the nodes copy it successfully");
    }

    private void processReplicateResponse(final String remoteAddr, AppendEntriesResponse response) {
        if(response.isSuccess()) {
            // set nextIndex
            nodeStatus.updatePeerNodeStatus(remoteAddr,
                    response.getMatchIndex() + 1, response.getMatchIndex());
        } else { // replicate failed
            switch (response.getFailedType()) {
                case EXCEPTION:
                    break;
                case OLD_TERM:
                    break;
                case NOT_MATCH:
                    System.out.println(remoteAddr + "->" + nodeStatus.getNodeStatus(remoteAddr).nextIndex);
                    nodeStatus.decrementNextIndex(remoteAddr);
                    break;
            }

        }
    }

    //prepare Entry to replicate
    private CommitLogEntry prepareEntryByAddr(int nextIdx) {
        CommitLogEntry entry = null;
        try {
            // nextId对应位置可能没有item
            if(nextIdx < storeManager.getLogQueue().size()) {
                entry = storeManager.getByIndex(nextIdx);
            }
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




    class ElectionTask implements Runnable {
        @Override
        public void run() {
            LOG.debug("election start...");
            // 每次选举开始，赋予投票权
            NodeManager.this.nodeStatus.resetVoteRight();

            final AtomicInteger voteCount = new AtomicInteger(0);
            final CountDownLatch latch = new CountDownLatch(addrList.size() - 1);

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

            //检查状态
            if(nodeStatus.getStatus().equals(NodeStatus.STATUS.FOLLOWER)) {
                // 已经出现了Leader, 选举结束
                LOG.info("the leader has been elected, end election");
                return ;
            }
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

            LOG.debug("node {} receive {} notes", selfAddr, voteCount.get());
            if(nodeStatus.getStatus().equals(NodeStatus.STATUS.FOLLOWER)) {
                // 已经出现了Leader, 选举结束
                LOG.info("the leader has been elected, end election");
                return ;
            }
            if (voteCount.get() > addrList.size() / 2) {
                LOG.info("the node {} receive {} votes, then turn to the leader", selfAddr, voteCount.get());
                NodeManager.this.setNodeStatus(NodeStatus.STATUS.LEADER);
                NodeManager.this.setLeader(selfAddr);
                // 不再进行定时选举
                NodeManager.this.randomResettableTimer.clearTimer();

                //======= init node status in every election =======
                int leaderLastLogIndex = storeManager.getLogQueue().size() - 1;
                NodeManager.this.initNodeStatus(leaderLastLogIndex);

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

            LOG.debug("the node {} receive {} notes, but can not ahead the majority, do nothing", selfAddr, voteCount.get());
            LOG.debug("end election...");
        }

    }

    /**
     * rpc invoke
     */
    class  ConsensusServiceImpl implements ConsensusService {

        @Override
        public VoteResponse invokeVote(final String addr, final VoteRequest voteRequest, long timeoutMillis) throws RegistrationVoteException {

            RemotingCommand req = RemotingCommand.createRequest(RequestCode.REIGISTRATION_REQUESTVOTE);
            req.setBody(
                    JSONSerializer.encode(voteRequest)
            );

            try {
                RemotingCommand response = NodeManager.this.nettyRemotingClient.invokeSync(addr, req, timeoutMillis);
                if (response == null) {
                    LOG.warn("the remote server {} maybe has a problem", addr);
                    throw new RegistrationVoteException("the remote node {} response nothing", addr);
                }
                return JSONSerializer.decode(response.getBody(), VoteResponse.class);
            } catch (InterruptedException e) {
                LOG.error("an error occurred in election, check the remote addr {}", addr, e);
                throw new RegistrationVoteException(addr, "exception occurred when vote to other node");
            } catch (RemotingConnectException | RemotingSendRequestException | RemotingTimeoutException ex) {
                LOG.error("invoke error send vote to {} request exception", addr, ex);
                throw new RegistrationVoteException(addr, "remoting exception occurred");
            }
        }

        @Override
        public AppendEntriesResponse replicateLog(String addr, AppendEntriesRequest appendEntriesRequest,
                                                  long timeoutMillis, boolean isHeartbeat) throws ReplicatedLogException {
            LOG.debug("heartbeat start to send...");
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
                RemotingCommand response = NodeManager.this.nettyRemotingClient.invokeSync(addr, req, timeoutMillis);
                if (response != null) {
                    return JSONSerializer.decode(response.getBody(), AppendEntriesResponse.class);
                } else {
                    LOG.error("the request to the remote node {} timeout", addr);
                }
            } catch (InterruptedException e) {
                LOG.error("an error occurred in election, check the remote addr {}", addr, e);
            } catch (RemotingConnectException | RemotingSendRequestException | RemotingTimeoutException ex) {
                LOG.error("invoke error send vote to {} request exception", addr, ex);
            }
            throw new ReplicatedLogException(String.format("error occurred in replicate Log to %s", addr));
        }
    }


    /**
     * this task is the core task in replicating
     * it will be responsible for two tasks:
     *      1.reset the follower's election timer
     *      2.replicate the log and keep the consistency. Made the follower's status as the same as the leader
     */
    class HeartBeatTask implements Runnable {

        @Override
        public void run() {
            final ExecutorService publicThreadPool = NodeManager.this.publicThreadPool;
            final Set<String> addrs = NodeManager.this.addrList;

            for (String addr :
                    addrs) {
                if (addr.equals(selfAddr)) {
                    continue;
                }
                publicThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            NodeStatus.InnerPeerNodeStatus status = nodeStatus.getNodeStatus(addr);
                            // 获取需要复制的log
                            LOG.debug("addr [{}] -> status: [{}]", addr, status);
                            final AppendEntriesRequest heartbeat = prepareEmptyRequest(status.nextIndex);
                            CommitLogEntry entryByNodeAddr = prepareEntryByAddr(status.nextIndex);

                            heartbeat.setCommitLogEntries(Collections.singletonList(entryByNodeAddr));

                            AppendEntriesResponse response = consensusService.replicateLog(addr, heartbeat,
                                    300, true);
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
        }
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

    public void clearHeartBeatTask() {
        final BlockingQueue<Runnable> taskQueue = this.heartBeatTimer.getQueue();
        if(!taskQueue.isEmpty()) {
            taskQueue.clear();
        }
    }

    public NodeStatus.STATUS getNodeStatus() {
        return nodeStatus.getStatus();
    }

    public boolean isVoteAvailable() {
        return nodeStatus.isVoteAvailable();
    }
    public void setVoteAvailable(boolean available) {
        nodeStatus.setVoteAvailable(available);
    }

    public String getSelfAddr() {
        return selfAddr;
    }

    public void setLeader(String leaderAddr) {
        this.nodeStatus.setLeaderAddr(leaderAddr);
    }

    public String getLeader() {
        return this.nodeStatus.getLeaderAddr();
    }

    public NettyRemotingClient getNettyRemotingClient() {
        return nettyRemotingClient;
    }
}
