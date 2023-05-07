package com.tinymq.core.processor;

import com.tinymq.common.protocol.RequestCode;
import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.dto.outer.StateModel;
import com.tinymq.core.status.NodeManager;
import com.tinymq.core.store.CommitLogEntry;
import com.tinymq.core.store.StoreManager;
import com.tinymq.remote.exception.RemotingConnectException;
import com.tinymq.remote.exception.RemotingSendRequestException;
import com.tinymq.remote.exception.RemotingTimeoutException;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcceptClientProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AcceptClientProcessor.class);
    private final NodeManager nodeManager;
    private final StoreManager storeManager;

    public AcceptClientProcessor(final NodeManager nodeManager, final StoreManager storeManager) {
        this.nodeManager = nodeManager;
        this.storeManager = storeManager;
    }

    @Override
    public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
        if(request == null) {
            LOG.info("the node {} receive null request", nodeManager.getSelfAddr());
            return null;
        }

        /*  */
        switch (nodeManager.getNodeStatus()) {
            case LEADER:
                return process0(request);
            case CANDIDATE:
                // do not handle request
                return RemotingCommand.createResponse(
                        request.getCode(),"the server {} reject the request, the status is [CANDIDATE]");
            case FOLLOWER:
                // redirect
                return processRedirect(request);
            default:
                break;
        }

        return null;
    }

    private RemotingCommand processRedirect(RemotingCommand request) {
        return nodeManager.redirectToLeader(request);
    }

    private RemotingCommand process0(RemotingCommand request) {
        switch (request.getCode()) {
            case RequestCode.REGISTRATION_CLIENT_READ:
                processReadImpl(request);
                break;
            case RequestCode.REGISTRATION_CLIENT_WRITE:
                processWriteImpl(request);
                break;
            default:
                break;
        }
        LOG.error("do not support the request code {}", request.getCode());
        return RemotingCommand.createResponse(request.getCode(), "do not support this request code");
    }

    private void processReadImpl(RemotingCommand request) {

    }

    private RemotingCommand processWriteImpl(RemotingCommand request) {
        StateModel stateModel;
        try {
            stateModel = JSONSerializer.decode(request.getBody(), StateModel.class);
        } catch (Exception e) {
            LOG.error("the server require the StateModel.class");
            return RemotingCommand.createResponse(request.getCode(), "the server require the StateModel.class");
        }

        try {
            RemotingCommand resp = this.nodeManager.handleRequest(stateModel);
            //TODO: 只要保存到本地, 同步log成功，就返回，不等commit

            return RemotingCommand.createResponse(request.getCode(), "save success");
        } catch (Exception e) {
            LOG.error("replicate log error");
            return RemotingCommand.createResponse(request.getCode(), "replicate log error");
        }
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
