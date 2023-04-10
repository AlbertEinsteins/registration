package com.tinymq.remote.exception;

public class RemotingException extends Exception {
    public RemotingException() {}

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemotingException(String message) {
        super(message);
    }
    public RemotingException(Throwable cause) {
        super(cause);
    }
}
