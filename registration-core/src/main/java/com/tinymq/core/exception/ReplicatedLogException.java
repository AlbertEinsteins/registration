package com.tinymq.core.exception;

import com.tinymq.core.dto.AppendEntriesRequest;

public class ReplicatedLogException extends Exception {
    private final AppendEntriesRequest request;

    public ReplicatedLogException(final String msg, final AppendEntriesRequest request) {
        super(msg);
        this.request = request;
    }

}
