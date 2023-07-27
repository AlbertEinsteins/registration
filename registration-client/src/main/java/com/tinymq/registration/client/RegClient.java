package com.tinymq.registration.client;

import com.tinymq.registration.client.dto.SendResult;
import com.tinymq.registration.client.exception.SendException;


public interface RegClient {
    /**
     * 根据key获取, sync method
     *
     * @param key key: string
     * @return SendResult
     */
    SendResult get(String key) throws SendException;

    /**
     * sync method
     *
     * @param key keyname: string
     * @param val value: string in normal
     * @return SendResult
     */
    SendResult put(String key, String val) throws SendException;


    /**
     * 添加监听器
     *
     * @param key
     * @return
     */
    SendResult addWatcher(String key, InvokeCallback invokeCallback);

    SendResult createNode(String key);

    void addServerNodes(String... nodes);

    void start();

    void shutdown();
}
