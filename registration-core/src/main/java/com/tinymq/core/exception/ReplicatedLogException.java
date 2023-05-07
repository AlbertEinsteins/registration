package com.tinymq.core.exception;

import com.tinymq.core.dto.AppendEntriesRequest;

public class ReplicatedLogException extends Exception {

    public ReplicatedLogException(final String msg) {
        super(msg);
    }

}
