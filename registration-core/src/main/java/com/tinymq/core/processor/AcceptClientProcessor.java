package com.tinymq.core.processor;

import com.tinymq.common.dto.WatcherDto;
import com.tinymq.common.protocol.ExtFieldDict;
import com.tinymq.common.protocol.RequestCode;
import com.tinymq.common.protocol.RequestStatus;
import com.tinymq.core.exception.KeyNotFoundException;
import com.tinymq.core.status.NodeManager;
import com.tinymq.core.status.StateMachine;
import com.tinymq.core.watcher.Watcher;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AcceptClientProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AcceptClientProcessor.class);
    private final NodeManager nodeManager;

    private final StateMachine stateMachine;

    public AcceptClientProcessor(final NodeManager nodeManager, final StateMachine stateMachine) {
        this.nodeManager = nodeManager;
        this.stateMachine = stateMachine;
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
                // do not handle request, response leader ip
                return RemotingCommand.createResponse(
                        request.getCode(),"the server {} reject the request, the status is [CANDIDATE]");
            case FOLLOWER:
                // redirect
                return processFollower(request);
            default:
                break;
        }

        return null;
    }

    private RemotingCommand processFollower(RemotingCommand request) {
        // 如果是Follower，那么会送客户端leader的地址，让客户端去访问
        String leaderAddr = nodeManager.getLeader();
        LOG.debug("the client request non-leader node, leaderip [{}]", leaderAddr);
        final RemotingCommand resp = RemotingCommand.createResponse(request.getCode(), "the request node is not leader, return leader ip",
                setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.NOT_LEADER));
        resp.setBody(
                leaderAddr.getBytes(StandardCharsets.UTF_8)
        );
        return resp;
    }

    private RemotingCommand process0(RemotingCommand request) {
        try {
            switch (request.getCode()) {
                case RequestCode.REGISTRATION_CLIENT_READ:
                    return processReadImpl(request);
                case RequestCode.REGISTRATION_CLIENT_WRITE:
                    return processWriteImpl(request);
                case RequestCode.REGISTRATION_CLIENT_WATCHER_ADD:
                case RequestCode.REGISTRATION_CLIENT_WATCHER_DEL:
                    return processRegist(request);
                case RequestCode.REGISTRATION_CLIENT_KEY_CREATE:
                    return processNodeAdd(request);
                default:
                    break;
            }
        } catch (Exception e) {
            LOG.error("handle req code:[{}], error occurred", request.getCode(), e);
        }
        return RemotingCommand.createResponse(request.getCode(), "error occurred when handle request");
    }

    private RemotingCommand processNodeAdd(RemotingCommand request) {
        WatcherDto watcherDto;
        try {
            watcherDto = JSONSerializer.decode(request.getBody(), WatcherDto.class);
        } catch (Exception e) {
            return RemotingCommand.createResponse(request.getCode(),
                    "Error occurred when parse body, it should be WatcherDto.class",
                    setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.EXCEPTION_OCCURRED));
        }

        String key = watcherDto.getWatcherKey();
        try {
            this.stateMachine.createNode(key);
            return RemotingCommand.createResponse(request.getCode(),
                    "create successful", setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.WRITE_SUCCESS));
        } catch (Exception e) {
            return RemotingCommand.createResponse(request.getCode(),
                    e.getMessage(), setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.EXCEPTION_OCCURRED));
        }
    }

    private RemotingCommand processRegist(RemotingCommand request) {
        // extract key, client
        WatcherDto watcherDto;
        try {
            watcherDto = JSONSerializer.decode(request.getBody(), WatcherDto.class);
        } catch (Exception e) {
            return RemotingCommand.createResponse(request.getCode(),
                    "Error occurred when parse body, it should be WatcherDto.class",
                    setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.EXCEPTION_OCCURRED));
        }

        String key = watcherDto.getWatcherKey();
        String clientAddr = watcherDto.getClientAddr();

        try {
            this.stateMachine.registryWatcher(key, clientAddr);
            return RemotingCommand.createResponse(request.getCode(), "register successfully",
                    setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.WRITE_SUCCESS));
        } catch (KeyNotFoundException re) {
            return RemotingCommand.createResponse(request.getCode(), re.getMsg(),
                    setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.KEY_NOT_EXIST));
        }
    }

    private RemotingCommand processReadImpl(RemotingCommand request) {
        final RemotingCommand resp = nodeManager.handleReadRequest(request);
        Map<String, String> extFields = new HashMap<>();
        extFields.put(ExtFieldDict.REGISTRY_REQUEST_STATUS, String.valueOf(RequestStatus.READ_SUCCESS.code));
        resp.setExtFields(extFields);
        return resp;
    }

    private RemotingCommand processWriteImpl(RemotingCommand request) {
        try {
            stateMachine.decode(request.getBody());
        } catch (Exception e) {
            LOG.error("the server require the KVStateModel.class", e);
            return RemotingCommand.createResponse(request.getCode(), "the server require the StateModel.class",
                    setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.EXCEPTION_OCCURRED));
        }

        try {
            RemotingCommand resp = this.nodeManager.handleWriteRequest(request);
            //只要保存到本地, 同步log成功，并写入状态机
            resp.setExtFields(setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.WRITE_SUCCESS));
            return resp;
        } catch (Exception e) {
            LOG.error("replicate log error", e);
            return RemotingCommand.createResponse(request.getCode(), "replicate log error",
                    setRequestStatus(ExtFieldDict.REGISTRY_REQUEST_STATUS, RequestStatus.EXCEPTION_OCCURRED));
        }
    }

    private Map<String, String> setRequestStatus(String extPropertyName, RequestStatus requestStatus) {
        if(requestStatus != null) {
            Map<String, String> extFields = new HashMap<>();

            extFields.put(extPropertyName, String.valueOf(requestStatus.code));
            return extFields;
        }
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
