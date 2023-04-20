package com.tinymq.core.processor;

import com.tinymq.common.protocol.RequestCode;
import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.dto.AppendEntriesResponse;
import com.tinymq.core.exception.AppendLogException;
import com.tinymq.core.status.NodeManager;
import com.tinymq.core.status.NodeStatus;
import com.tinymq.core.store.DefaultLogQueue;
import com.tinymq.core.store.StoreManager;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
            // old-term node request
            return rejectOldTermReq(request, appendEntriesRequest.getTerm());
        }

        switch (request.getCode()) {
            case RequestCode.APPENDENTRIES_EMPTY:
                return processEmptyHeartbeat(appendEntriesRequest, ctx);
            case RequestCode.APPENDENTRIES:
                return processAppendEntries(appendEntriesRequest, ctx, request);
            default:
                break;
        }

        LOG.info("receive heartbeat request which request code {} is missing the processor", request.getCode());
        return null;
    }

    /**
     * 对于普通心跳，简单log，重置自己的定时器，设置为Follower状态
     */
    private RemotingCommand processEmptyHeartbeat(AppendEntriesRequest req, ChannelHandlerContext ctx) {
        LOG.info("receive heartbeat in term {} from other node , cur node term {}", req.getTerm(), nodeManager.getCurTerm());
        this.nodeManager.resetElectionTimer();
        this.nodeManager.setLeader(req.getLeaderAddr());
        this.nodeManager.setNodeStatus(NodeStatus.STATUS.FOLLOWER);
        return null;
    }


    private RemotingCommand processAppendEntries(AppendEntriesRequest req, ChannelHandlerContext ctx, RemotingCommand request) {
        // TODO: 检查，回退log到与leader一致的时刻
        final DefaultLogQueue logQueue = storeManager.getLogQueue();
        final int prevLogIndex = req.getPrevLogIndex();
        final int prevLogTerm = req.getPrevLogTerm();

        try {
            if(logQueue.size() == 0
                || logQueue.at(prevLogIndex).getTerm() == prevLogTerm) {
                //create
                storeManager.appendLog(req);
                RemotingCommand resp = RemotingCommand.createResponse(request.getCode(), "success");
                resp.setBody(
                        JSONSerializer.encode(AppendEntriesResponse.create(nodeManager.getCurTerm(), true))
                );
                return resp;
            }

            // TODO: not match
            LOG.info("the node {} not match prevLogIndex {} with prevLogTerm {}; [curTerm: {}, curLogIndex: {}]",
                    nodeManager.getSelfAddr(), prevLogIndex, prevLogTerm,
                    nodeManager.getCurTerm(), storeManager.getLogQueue().size() - 1);

        } catch (AppendLogException e) {
            throw new RuntimeException(e);
        }


        return null;
    }

    private RemotingCommand rejectOldTermReq(RemotingCommand req, int reqTerm) {
        RemotingCommand response = RemotingCommand.createResponse(req.getCode(),
                String.format("the req term {%d} is less than the peer node {%d}", reqTerm, nodeManager.getCurTerm()));
        response.setBody(
                JSONSerializer.encode(AppendEntriesResponse.create(nodeManager.getCurTerm(), false))
        );
        return response;
    }


    @Override
    public boolean rejectRequest() {
        return false;
    }
}
