package com.tinymq.core;

import com.tinymq.core.store.CommitLogEntry;

public interface StateMachine {

    /**
     * 将某条entry 写到状态机里
     * @param logEntry
     */
    void execute(final CommitLogEntry logEntry);
}
