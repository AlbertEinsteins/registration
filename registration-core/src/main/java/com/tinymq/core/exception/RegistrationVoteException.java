package com.tinymq.core.exception;


public class RegistrationVoteException extends Exception {
    private String msg;
    private String remoteAddr;

    public RegistrationVoteException(String remoteAddr, String msg) {
        this.msg = msg;
        this.remoteAddr = remoteAddr;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }
}
