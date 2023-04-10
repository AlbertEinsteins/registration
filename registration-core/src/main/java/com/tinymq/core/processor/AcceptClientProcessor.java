package com.tinymq.core.processor;

import com.tinymq.core.status.NodeManager;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcceptClientProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AcceptClientProcessor.class);

    private final NodeManager nodeManager;

    public AcceptClientProcessor(final NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
