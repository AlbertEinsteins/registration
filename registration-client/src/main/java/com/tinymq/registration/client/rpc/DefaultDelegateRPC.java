package com.tinymq.registration.client.rpc;

import com.tinymq.common.protocol.ExtFieldDict;
import com.tinymq.common.protocol.RequestStatus;
import com.tinymq.core.status.KVStateModel;
import com.tinymq.registration.client.dto.SendRequest;
import com.tinymq.registration.client.dto.SendResult;
import com.tinymq.registration.client.exception.SendException;
import com.tinymq.remote.RemotingClient;
import com.tinymq.remote.exception.RemotingConnectException;
import com.tinymq.remote.exception.RemotingSendRequestException;
import com.tinymq.remote.exception.RemotingTimeoutException;
import com.tinymq.remote.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DefaultDelegateRPC implements DelegateRPC {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDelegateRPC.class);

    private final RemotingClient remotingClient;

    private RPCCallback<SendResult> rpcCallback;

    public DefaultDelegateRPC(RemotingClient remotingClient) {
        this(remotingClient, null);
    }

    public DefaultDelegateRPC(final RemotingClient remotingClient, RPCCallback<SendResult> resultRPCCallback) {
        this.remotingClient = remotingClient;
        this.rpcCallback = resultRPCCallback;
    }

    public void setRpcCallback(RPCCallback<SendResult> rpcCallback) {
        this.rpcCallback = rpcCallback;
    }

    @Override
    public SendResult send(SendRequest sendRequest, long timeoutMillis) throws SendException {
        if(sendRequest == null) {
            return SendResult.create(true, null, "Empty, do nothing", null);
        }

        RemotingCommand req = RemotingCommand.createRequest(sendRequest.getRequestCode());
        setRequestBody(req, sendRequest);
        final SendResult sendResult = getResponse(sendRequest, timeoutMillis, req);
        if(this.rpcCallback != null) {
            // after call
            this.rpcCallback.doOperate(sendResult);
        }
        return sendResult;
    }

    private SendResult getResponse(SendRequest sendRequest, long timeoutMillis, RemotingCommand req) throws SendException {
        try {
            RemotingCommand resp = this.remotingClient.invokeSync(sendRequest.getAddr(), req, timeoutMillis);
            final Map<String, String> extFields = resp.getExtFields();

            int status = Integer.parseInt(extFields.get(ExtFieldDict.REGISTRY_REQUEST_STATUS));
            return SendResult.create(true, resp.getBody(), resp.getInfo(),
                    RequestStatus.fromCode(status));
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException |
                 InterruptedException e) {
            throw new SendException("Runtime exception", e);
        }
    }


    private void setRequestBody(RemotingCommand req, SendRequest sendRequest) {
        req.setBody(
                sendRequest.getBody()
        );
    }

}