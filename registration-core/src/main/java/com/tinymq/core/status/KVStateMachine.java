package com.tinymq.core.status;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.lang.Assert;
import com.tinymq.core.exception.KeyNotFoundException;
import com.tinymq.core.store.CommitLogEntry;
import com.tinymq.core.watcher.Watcher;
import com.tinymq.core.watcher.WatcherListener;
import com.tinymq.remote.netty.NettyRemotingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class KVStateMachine implements StateMachine {
    private static final Logger LOG = LoggerFactory.getLogger(KVStateMachine.class);

    private final KVStateModel kvStateModelTemplate;

    protected final ReentrantLock lockState = new ReentrantLock();
    protected final ConcurrentHashMap<String /*key*/, String> state = new ConcurrentHashMap<>(32);

    protected final ConcurrentHashMap<String /*key*/, ConcurrentHashSet<Watcher>> watcherMap = new ConcurrentHashMap<>(32);

    protected WatcherListener watcherListener;

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

        boolean isUpdate = false;
        // 写入数据
        if(stateModel != null) {
            String k = stateModel.getKey();
            String v = stateModel.getVal();
            String oldVal = null;

            if(state.containsKey(k)) {
                try {
                    lockState.lock();
                    oldVal = state.get(k);
                    state.put(k, v);
                    isUpdate = true;
                } finally {
                    lockState.unlock();
                }
            } else {
                state.putIfAbsent(k, v);
            }

            if(isUpdate) {
                this.watcherListener.onKeyUpdate(k, oldVal, v);
            }
        }
    }

    @Override
    public void registryWatcher(String key, String clientAddr) {
        final Object val = getByKey(key);
        if(val == null) {
            throw new KeyNotFoundException(key, String.format("the key {%s} not found.", key));
        }

        ConcurrentHashSet<Watcher> watchers = getSelectWatchers(key);

        watchers.add(new Watcher(clientAddr));
        LOG.debug("the client [{}] is watching at the key [{}]", clientAddr, key);
    }

    private synchronized ConcurrentHashSet<Watcher> getSelectWatchers(String key) {
        ConcurrentHashSet<Watcher> watchers = watcherMap.get(key);
        if(watchers == null) {
            watchers = new ConcurrentHashSet<>();
            watcherMap.put(key, watchers);
        }
        return watchers;
    }

    @Override
    public Set<Watcher> getWatchers(String key) {
        return this.getSelectWatchers(key);
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

    @Override
    public void createNode(String key) {
        if(isExist(key)) {
            return ;
        }
        synchronized (this) {
            LOG.debug("create node [key: {}]", key);
            this.state.put(key, "");
            this.watcherMap.put(key, new ConcurrentHashSet<>());
        }
    }

    @Override
    public boolean isExist(String key) {
        return this.state.containsKey(key);
    }

    @Override
    public void registryWatcherListener(WatcherListener listener) {
        this.watcherListener = listener;
    }
}
