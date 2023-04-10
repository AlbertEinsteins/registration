package com.tinymq.core.processor;

import cn.hutool.core.lang.Assert;
import com.tinymq.core.dto.VoteRequest;
import com.tinymq.core.dto.VoteResposne;
import com.tinymq.core.status.NodeManager;
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
            VoteResposne voteResposne = VoteResposne.createVote(-1, false);
            RemotingCommand resp = RemotingCommand.createResponse(request.getCode(),
                    String.format("reject the vote request in term {%d}", vote.getTerm()));
            resp.setBody(JSONSerializer.encode(voteResposne));
            return resp;
        }

        // 收到其他节点请求，重置自己的timer
        this.nodeManager.resetElectionTimer();

        LOG.info("node {} receive vote reuqest: {}", nodeManager.getSelfAddr(), vote);

        VoteResposne voteResposne = VoteResposne.createVote(nodeManager.getCurTerm(), true);
        RemotingCommand resp = RemotingCommand.createResponse(request.getCode(),
                String.format("the vote of the node {%s}", RemotingUtils.parseRemoteAddress(ctx.channel())));
        resp.setBody(JSONSerializer.encode(voteResposne));
        return resp;
    }


    @Override
    public boolean rejectRequest() {
        return false;
    }
}
