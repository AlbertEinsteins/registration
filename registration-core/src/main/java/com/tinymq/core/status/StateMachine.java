package com.tinymq.core.status;

import com.tinymq.core.store.CommitLogEntry;
import com.tinymq.core.watcher.Watcher;
import com.tinymq.core.watcher.WatcherListener;
import com.tinymq.remote.netty.NettyRemotingClient;

import java.util.List;
import java.util.Set;

public interface StateMachine {

    /**
     * 将某条Command 写到状态机里
     */
    void execute(CommitLogEntry commitLogEntry);

    boolean isExist(String key);

    void createNode(String key);

    void registryWatcher(String key, String clientAddr);

    void registryWatcherListener(WatcherListener listener);

    Set<Watcher> getWatchers(String key);

    /**
     * 根据key获取val
     */
    <T> T getByKey(String key);

    <T> T decode(byte[] bytes);

}
