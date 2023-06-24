package com.tinymq.registration.client;

import com.tinymq.registration.client.dto.SendResult;
import com.tinymq.registration.client.exception.SendException;

public interface RegClient {
    /**
     * 根据key获取, sync method
     * @param key key: string
     * @return SendResult
     */
    SendResult get(String key, long timeoutMillis) throws SendException;

    /**
     * sync method
     * @param key keyname: string
     * @param val value: string in normal
     * @return SendResult
     */
    SendResult put(String key, String val, long timeoutMillis) throws SendException;

}
