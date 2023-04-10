package com.tinymq.remote.exception;

public class RemotingSendRequestException extends RemotingException {
    private String addr;

    public RemotingSendRequestException(String addr, Throwable cause) {
        super(cause);
        this.addr = addr;
    }

    public RemotingSendRequestException(String message) {
        super(message);
    }
}
