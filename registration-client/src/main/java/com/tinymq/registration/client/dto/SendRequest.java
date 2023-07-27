package com.tinymq.registration.client.dto;


public class SendRequest {
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
    private byte[] body;


    public static SendRequest createPollReq(String url, int code, byte[] body) {
        SendRequest sendRequest = new SendRequest();
        sendRequest.markPoll();
        sendRequest.requestCode = code ;
        sendRequest.addr = url;
        sendRequest.body = body;
        return sendRequest;
    }

    public static SendRequest createPushReq(String url, int code, byte[] body) {
        SendRequest sendRequest = new SendRequest();
        sendRequest.markPush();
        sendRequest.requestCode = code;
        sendRequest.addr = url;
        sendRequest.body = body;
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

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

}
