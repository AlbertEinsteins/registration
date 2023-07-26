package com.tinymq.core.watcher;


public interface WatcherListener {

    void onKeyUpdate(String key, String oldVal, String newVal);

    void onKeyDel(String key, String lastVal);
}
