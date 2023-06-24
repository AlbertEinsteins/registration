package com.tinymq.core.status;

import cn.hutool.core.lang.Assert;
import com.tinymq.core.store.CommitLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class KVStateMachine implements StateMachine {
    private static final Logger LOG = LoggerFactory.getLogger(KVStateMachine.class);

    private final KVStateModel kvStateModelTemplate;

    protected final ReentrantLock lockState = new ReentrantLock();
    protected final ConcurrentHashMap<String, String> state = new ConcurrentHashMap<>(32);

    public KVStateMachine() {
        this.kvStateModelTemplate = new DefaultKVStateModel();
    }

    @Override
    public void execute(CommitLogEntry commitLogEntry) {
        // 解码出KVStateModel
        KVStateModel stateModel;
        try {
            stateModel = kvStateModelTemplate.decode(commitLogEntry.getBody());
        } catch (Exception e) {
            LOG.error("the state model decode from commit log error.", e);
            return;
        }
        //TODO: 写入数据
        if(stateModel != null) {
            String k = stateModel.getKey();
            String v = stateModel.getVal();

            if(state.containsKey(k)) {
                try {
                    lockState.lock();
                    state.put(k, v);
                } finally {
                    lockState.unlock();
                }
            } else {
                state.putIfAbsent(k, v);
            }
        }
    }

    @Override
    public <T> T getByKey(String key) {
        return (T) getState(key);
    }

    @Override
    public <T> T decode(byte[] bytes) {
        Assert.notNull(this.kvStateModelTemplate, "the method registerStateModel must be done");
        return (T) this.kvStateModelTemplate.decode(bytes);
    }

    public String getState(String key) {
        return state.get(key);
    }

    protected void clearState(String key) {
        this.state.remove(key);
    }
}
