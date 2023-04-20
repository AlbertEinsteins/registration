package com.tinymq.core.exception;

import com.tinymq.core.store.MappedFile;

public class AppendLogException extends Exception {
    private MappedFile mappedFile;

    public AppendLogException(MappedFile mappedFile, String msg, Throwable e) {
        super(msg, e);
        this.mappedFile = mappedFile;
    }

    public MappedFile getMappedFile() {
        return mappedFile;
    }

    public void setMappedFile(MappedFile mappedFile) {
        this.mappedFile = mappedFile;
    }
}
