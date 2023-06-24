package com.tinymq.registration.client.dto;

import com.tinymq.common.protocol.RequestStatus;

public class SendResult {
    private boolean isSuccess;
    private String info;
    private byte[] result;

    private RequestStatus status;

    public static SendResult create(boolean isSuccess, byte[] result, String info, RequestStatus status) {
        SendResult res = new SendResult();
        res.isSuccess = isSuccess;
        res.result = result;
        res.info = info;
        res.status = status;
        return res;
    }


    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public byte[] getResult() {
        return result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }
}
