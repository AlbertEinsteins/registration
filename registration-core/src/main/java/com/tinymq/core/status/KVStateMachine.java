package com.tinymq.core.status;

import com.tinymq.core.dto.outer.StateModel;
import com.tinymq.core.store.InstanceInfo;
import com.tinymq.core.store.CommitLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class KVStateMachine implements StateMachine {
    private static final Logger LOG = LoggerFactory.getLogger(KVStateMachine.class);
    /**
     *  <k,v>: <service-name, instanceInfo>
     *
     **/
    protected final ConcurrentHashMap<String, InstanceInfo> state = new ConcurrentHashMap<>(32);



    @Override
    public void execute(StateModel stateModel) {
        //TODO
    }

    public InstanceInfo getState(String key) {
        return state.get(key);
    }

    protected void updateState(String key, InstanceInfo instanceInfo) {
        InstanceInfo serviceInfo = state.get(key);
    }
    protected void clearState(String key) {
        this.state.remove(key);
    }
}
