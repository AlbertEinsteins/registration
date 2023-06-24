package com.tinymq.registration.client.dto;

import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.tinymq.common.protocol.RequestCode;
import com.tinymq.core.status.DefaultKVStateModel;
import com.tinymq.core.status.KVStateModel;

public class SendRequest {
    public static final int PUSH_REQUEST_CODE = RequestCode.REGISTRATION_CLIENT_WRITE;
    public static final int POLL_RESPONSE_CODE = RequestCode.REGISTRATION_CLIENT_READ;
    public static final int PUSH_FLAG = 0;
    public static final int POLL_FLAG = 1;

    /*ip:port*/
    private String addr;
    private int requestCode;
    /**
     *  send or get request
     *      0 for send req
     *      1 for get req
     */
    private int flag;
    private KVStateModel kvStateModel;


    public static SendRequest createPollReq(String url, String key) {
        SendRequest sendRequest = new SendRequest();
        sendRequest.markPoll();
        sendRequest.requestCode = POLL_RESPONSE_CODE;
        sendRequest.addr = url;
        sendRequest.kvStateModel = new DefaultKVStateModel();
        sendRequest.kvStateModel.setKey(key);
        return sendRequest;
    }

    public static SendRequest createPushReq(String url, String key, String val) {
        SendRequest sendRequest = new SendRequest();
        sendRequest.markPush();
        sendRequest.requestCode = PUSH_REQUEST_CODE;
        sendRequest.addr = url;
        sendRequest.kvStateModel = new DefaultKVStateModel();
        sendRequest.kvStateModel.setKey(key);
        sendRequest.kvStateModel.setVal(val);
        return sendRequest;
    }

    private SendRequest() {
    }

    private void markPoll() {
        this.flag = POLL_FLAG;
    }
    private void markPush() {
        this.flag = PUSH_FLAG;
    }

    public boolean isPollRequest() {
        return this.flag == POLL_FLAG;
    }
    public boolean isPushRequest() {
        return this.flag == PUSH_FLAG;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public KVStateModel getKvStateModel() {
        return kvStateModel;
    }

    public String getAddr() {
        return addr;
    }
}
