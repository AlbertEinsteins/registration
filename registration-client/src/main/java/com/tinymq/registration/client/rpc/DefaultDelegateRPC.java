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

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DefaultDelegateRPC implements DelegateRPC {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDelegateRPC.class);


    private final RemotingClient remotingClient;

    public DefaultDelegateRPC(final RemotingClient remotingClient) {
        this.remotingClient = remotingClient;
    }

    @Override
    public SendResult send(SendRequest sendRequest, long timeoutMillis, RPCCallback<SendResult> sendResultRPCCallback) throws SendException {
        if(sendRequest == null) {
            return SendResult.create(true, null, "Empty, do nothing", null);
        }

        RemotingCommand req = RemotingCommand.createRequest(sendRequest.getRequestCode());
        setRequestBody(req, sendRequest);
        return getResponse(sendRequest, timeoutMillis, req);
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
        KVStateModel state = sendRequest.getKvStateModel();

        switch (sendRequest.getRequestCode()) {
            case SendRequest.POLL_RESPONSE_CODE:
                req.setBody(
                        state.getKey().getBytes(StandardCharsets.UTF_8)
                );
                return ;
            case SendRequest.PUSH_REQUEST_CODE:
                req.setBody(
                        state.encode(state)
                );
                return ;
        }
        throw new RuntimeException("Not support code");
    }
}