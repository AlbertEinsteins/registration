package com.tinymq.remote.exception;

public class RemotingTimeoutException extends RemotingException {
    private String addr;
    private long timeoutMillis;
    public RemotingTimeoutException(String addr, long timeoutMillis, Throwable cause) {
        super(cause);
        this.addr = addr;
        this.timeoutMillis = timeoutMillis;
    }


    public RemotingTimeoutException(String message) {
        super(message);
    }
}
