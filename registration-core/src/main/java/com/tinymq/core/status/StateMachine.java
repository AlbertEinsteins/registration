package com.tinymq.core.status;

import com.tinymq.core.dto.outer.StateModel;
import com.tinymq.core.store.CommitLogEntry;

public interface StateMachine {

    /**
     * 将某条Command 写到状态机里
     * @param stateModel
     */
    void execute(final StateModel stateModel);
}
