package com.tinymq.core.status;

import com.tinymq.core.dto.outer.StateModel;
import com.tinymq.core.store.InstanceInfo;
import com.tinymq.core.store.CommitLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class KVStateMachine implements StateMachine {
    private static final Logger LOG = LoggerFactory.getLogger(KVStateMachine.class);
    /**
     *  <k,v>: <service-name, instanceInfo>
     *
     **/
    protected final ReentrantLock lockState = new ReentrantLock();
    protected final ConcurrentHashMap<String, String> state = new ConcurrentHashMap<>(32);



    @Override
    public void execute(StateModel stateModel) {
        //TODO: 写入数据
        if(stateModel != null) {
            Object k = stateModel.getKey();
            Object v = stateModel.getVal(k);

            if(state.containsKey(k)) {
                try {
                    lockState.lock();
                    state.put((String) k, (String) v);
                } finally {
                    lockState.unlock();
                }
            } else {
                state.putIfAbsent((String) k, (String) v);
            }
        }
    }

    public String getState(String key) {
        return state.get(key);
    }

    protected void clearState(String key) {
        this.state.remove(key);
    }
}
