package com.tinymq.core.status;

import com.tinymq.core.store.CommitLogEntry;

public interface StateMachine {

    /**
     * 将某条Command 写到状态机里
     */
    void execute(CommitLogEntry commitLogEntry);

    /**
     * 根据key获取
     */
    <T> T getByKey(String key);

    /**
     * 解码
     */
    <T> T decode(final byte[] bytes);
}
