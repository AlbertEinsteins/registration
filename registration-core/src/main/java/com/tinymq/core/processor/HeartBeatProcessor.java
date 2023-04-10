package com.tinymq.core.processor;

import com.tinymq.common.protocol.RequestCode;
import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.status.NodeManager;
import com.tinymq.core.status.NodeStatus;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;


public class HeartBeatProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(HeartBeatProcessor.class);

    private final NodeManager nodeManager;
    public HeartBeatProcessor(final NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }


    @Override
    public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
        if(request == null) {
            LOG.error("receive a null RemotingCommand, please check what' s up.");
            return null;
        }
        AppendEntriesRequest appendEntriesRequest = JSONSerializer.decode(request.getBody(), AppendEntriesRequest.class);
        switch (request.getCode()) {
            case RequestCode.APPENDENTRIES_EMPTY:
                return processEmptyHeartbeat(appendEntriesRequest, ctx);
            case RequestCode.APPENDENTRIES:
                return processAppendEntries(appendEntriesRequest, ctx);
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

        if(req.getTerm() >= this.nodeManager.getCurTerm()) {
            this.nodeManager.resetElectionTimer();
            this.nodeManager.setLeader(req.getLeaderAddr());
            this.nodeManager.setNodeStatus(NodeStatus.STATUS.FOLLOWER);
        } else {
          // still in state CANDIDATE
        }
        return null;
    }


    private RemotingCommand processAppendEntries(AppendEntriesRequest req, ChannelHandlerContext ctx) {
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
