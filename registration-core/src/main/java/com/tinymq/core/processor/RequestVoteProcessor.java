package com.tinymq.core.processor;

import cn.hutool.core.lang.Assert;
import com.tinymq.core.dto.VoteRequest;
import com.tinymq.core.dto.VoteResponse;
import com.tinymq.core.status.NodeManager;
import com.tinymq.core.status.NodeStatus;
import com.tinymq.remote.common.RemotingUtils;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestVoteProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RequestVoteProcessor.class);
    
    private final NodeManager nodeManager;
    
    public RequestVoteProcessor(final NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }
    
    @Override
    public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
        Assert.notNull(request, "vote request can not be null");
        if(nodeManager.getNodeStatus().equals(NodeStatus.STATUS.CANDIDATE)) {
            //do not vote for anyone in candidate
            VoteResponse voteResponse = VoteResponse.createVote(nodeManager.getCurTerm(), false);
            RemotingCommand resp = RemotingCommand.createResponse(request.getCode(),
                    "reject the vote request in which the peer node is in candidate");
            resp.setBody(JSONSerializer.encode(voteResponse));
            return resp;
        }

        VoteRequest vote = JSONSerializer.decode(request.getBody(), VoteRequest.class);

        if(vote.getTerm() < nodeManager.getCurTerm()) {
            VoteResponse voteResponse = VoteResponse.createVote(nodeManager.getCurTerm(), false);
            RemotingCommand resp = RemotingCommand.createResponse(request.getCode(),
                    String.format("reject the vote request in term {%d}", vote.getTerm()));
            resp.setBody(JSONSerializer.encode(voteResponse));
            return resp;
        }


        // TODO: 只能投票一次
        // 收到其他节点请求，重置自己的timer
        VoteResponse voteResponse = null;
        if(vote.getTerm() == nodeManager.getCurTerm()) {
            // voted already
            voteResponse = VoteResponse.createVote(nodeManager.getCurTerm(), false);
        } else {
            this.nodeManager.resetElectionTimer();
            this.nodeManager.setCurTerm(vote.getTerm());

            voteResponse = VoteResponse.createVote(nodeManager.getCurTerm(), true);
        }

        LOG.info("node {} receive vote request: {}", nodeManager.getSelfAddr(), vote);
        RemotingCommand resp = RemotingCommand.createResponse(request.getCode(),
                String.format("the vote of the node {%s}", RemotingUtils.parseRemoteAddress(ctx.channel())));
        resp.setBody(JSONSerializer.encode(voteResponse));
        return resp;
    }


    @Override
    public boolean rejectRequest() {
        return false;
    }
}
