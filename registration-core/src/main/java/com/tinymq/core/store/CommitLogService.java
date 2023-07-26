package com.tinymq.core.store;

import com.tinymq.core.exception.AppendLogException;

public interface CommitLogService {
    //新增，返回存储的位置(下标)
    long appendLog(final CommitLogEntry commitLogEntry) throws AppendLogException;


    //
    CommitLogEntry getByOffset(final int offset);
}
