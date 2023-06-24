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
        VoteRequest vote = JSONSerializer.decode(request.getBody(), VoteRequest.class);

        if(vote.getTerm() < nodeManager.getCurTerm()) {
            //reject old term
            VoteResponse voteResponse = VoteResponse.createVote(nodeManager.getCurTerm(), false);
            RemotingCommand resp = RemotingCommand.createResponse(request.getCode(),
                    String.format("reject the vote request in term {%d}, cur term {%d}", vote.getTerm(), nodeManager.getCurTerm()));
            resp.setBody(JSONSerializer.encode(voteResponse));
            return resp;
        } else if(vote.getTerm() > nodeManager.getCurTerm()) {

            // get the new term vote, then cur node turns to follower
            LOG.debug("vote for node: {}", vote.getCandidateAddr());
            // 收到其他，投票
            nodeManager.setCurTerm(vote.getTerm());
            return getRemotingCommand(request, true, String.format("vote for node: {%s}", vote.getCandidateAddr()));
        } else {
            // so the cur node is in candidate.
            return getRemotingCommand(request, false, String.format("vote for node: {%s}", vote.getCandidateAddr()));
        }

    }

    private RemotingCommand getRemotingCommand(RemotingCommand request, boolean isVote, String info) {
        VoteResponse voteResponse = VoteResponse.createVote(nodeManager.getCurTerm(), isVote);
        RemotingCommand resp = RemotingCommand.createResponse(request.getCode(), info);
        resp.setBody(JSONSerializer.encode(voteResponse));
        return resp;
    }


    @Override
    public boolean rejectRequest() {
        return false;
    }


}
