package com.tinymq.core.processor;

import com.tinymq.common.protocol.RequestCode;
import com.tinymq.core.dto.AppendEntriesFailedType;
import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.dto.AppendEntriesResponse;
import com.tinymq.core.exception.AppendLogException;
import com.tinymq.core.status.NodeManager;
import com.tinymq.core.status.NodeStatus;
import com.tinymq.core.store.DefaultLogQueue;
import com.tinymq.core.store.StoreManager;
import com.tinymq.remote.netty.AsyncRequestProcessor;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.netty.ResponseCallBack;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AppendEntriesProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AppendEntriesProcessor.class);

    private final NodeManager nodeManager;
    private final StoreManager storeManager;


    public AppendEntriesProcessor(final NodeManager nodeManager,
                                  final StoreManager storeManager) {
        this.nodeManager = nodeManager;
        this.storeManager = storeManager;
    }


    @Override
    public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
        if(request == null) {
            LOG.error("receive a null RemotingCommand, please check what' s up.");
            return null;
        }
        AppendEntriesRequest appendEntriesRequest = JSONSerializer.decode(request.getBody(), AppendEntriesRequest.class);
        if(appendEntriesRequest.getTerm() < nodeManager.getCurTerm()) {
            // if old-term node request, reject
            return rejectOldTermReq(request, appendEntriesRequest.getTerm());
        } else if(appendEntriesRequest.getTerm() > nodeManager.getCurTerm()) {
            // the cur node turn to follower
            this.nodeManager.clearHeartBeatTask();
            this.nodeManager.setCurTerm(appendEntriesRequest.getTerm());
        }

        //重置自己的定时器
        this.nodeManager.resetElectionTimer();
        switch (request.getCode()) {
            case RequestCode.APPENDENTRIES_EMPTY:
                return processEmptyHeartbeat(appendEntriesRequest, request);
            case RequestCode.APPENDENTRIES:
                return processAppendEntries(appendEntriesRequest, request);
            default:
                break;
        }

        LOG.info("receive heartbeat request which request code {} is missing the processor", request.getCode());
        return null;
    }

    /**
     * 对于普通心跳，简单log，设置为Follower状态
     *          1.提交上次添加的日志
     *          2.如果有节点才上线，添加日志并提交
     */
    private RemotingCommand processEmptyHeartbeat(AppendEntriesRequest req, RemotingCommand request) {
        LOG.debug("receive heartbeat in term {} from other node , cur node term {}", req.getTerm(), nodeManager.getCurTerm());

        this.nodeManager.setLeader(req.getLeaderAddr());
        this.nodeManager.setNodeStatus(NodeStatus.STATUS.FOLLOWER);

        LOG.debug("Leader CommitIDx: " + req.getLeaderCommitIndex() + ", size: " + req.getCommitLogEntries().size());

        // find the match index
        final int prevLogTerm = req.getPrevLogTerm();
        final int prevLogIndex = req.getPrevLogIndex();

        AppendEntriesResponse response = null;
        if(storeManager.isMatch(prevLogIndex, prevLogTerm)) {
            int matchIndex = prevLogIndex;
            try {
                if(req.getCommitLogEntries() != null && !req.getCommitLogEntries().isEmpty()) {
                    storeManager.appendLog(req.getCommitLogEntries());
                    matchIndex ++;
                }
            } catch (Exception e) {
                LOG.error("exception", e);
                response = AppendEntriesResponse.create(this.nodeManager.getCurTerm(),
                        false, -1, AppendEntriesFailedType.EXCEPTION);
                RemotingCommand resp = RemotingCommand.createResponse(request.getCode(), String.format("node {%s} append log exception when in replicating the log.", nodeManager.getSelfAddr()));
                resp.setBody(JSONSerializer.encode(response));
                return resp;
            }
            LOG.debug("matchIndex: " + matchIndex + ", commitIdx: " + req.getLeaderCommitIndex());
            storeManager.setCommitIndexAndExec(req.getLeaderCommitIndex());

            response = AppendEntriesResponse.create(this.nodeManager.getCurTerm(),
                    true, matchIndex, null);
        } else {
            response = AppendEntriesResponse.create(this.nodeManager.getCurTerm(),
                    false, -1, AppendEntriesFailedType.NOT_MATCH);
        }
        RemotingCommand resp = RemotingCommand.createResponse(request.getCode(), "success");

        resp.setBody(JSONSerializer.encode(response));
        return resp;
    }


    //只添加，不做commit
    private RemotingCommand processAppendEntries(AppendEntriesRequest req, RemotingCommand request) {
        LOG.debug("receive append entry in term {} from other node , cur node term {}", req.getTerm(), nodeManager.getCurTerm());
        LOG.debug("Append:    " + req.getLeaderCommitIndex() + "->" + req.getCommitLogEntries().size());


        final int prevLogIndex = req.getPrevLogIndex();
        final int prevLogTerm = req.getPrevLogTerm();

        try {
            if(storeManager.isMatch(prevLogIndex, prevLogTerm)) {
                // store
                int matchIndex = storeManager.appendLog(req.getCommitLogEntries());
                RemotingCommand resp = RemotingCommand.createResponse(request.getCode(), "success");
                resp.setBody(
                        JSONSerializer.encode(
                                AppendEntriesResponse.create(nodeManager.getCurTerm(),
                                        true, matchIndex, null))
                );
                return resp;
            }

            // match failed, response to leader to decrement nextIndex of this node
            RemotingCommand resp = RemotingCommand.createResponse(request.getCode(), "the preLastLogIndex mismatch");
            resp.setBody(
                    JSONSerializer.encode(AppendEntriesResponse.create(nodeManager.getCurTerm(), false,
                            -1, AppendEntriesFailedType.NOT_MATCH))
            );
            return resp;
        } catch (AppendLogException e) {
            LOG.error("append log exception occurred", e);
            //the body is null if not set
            RemotingCommand resp = RemotingCommand.createResponse(request.getCode(), "replicate log error...");
            resp.setBody(
                    JSONSerializer.encode(AppendEntriesResponse.create(nodeManager.getCurTerm(), false, -1, AppendEntriesFailedType.EXCEPTION))
            );
            return resp;
        }
    }

    private RemotingCommand rejectOldTermReq(RemotingCommand req, int reqTerm) {
        RemotingCommand response = RemotingCommand.createResponse(req.getCode(),
                String.format("the req term {%d} is less than the cur node {%d}", reqTerm, nodeManager.getCurTerm()));
        response.setBody(
                JSONSerializer.encode(AppendEntriesResponse.create(nodeManager.getCurTerm(), false, -1, AppendEntriesFailedType.OLD_TERM))
        );
        return response;
    }


    @Override
    public boolean rejectRequest() {
        return false;
    }
}
