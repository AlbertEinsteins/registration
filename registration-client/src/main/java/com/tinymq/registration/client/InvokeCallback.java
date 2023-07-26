package com.tinymq.registration.client;

public interface InvokeCallback {
    void onKeyChanged(String key, String oldV, String newV) throws Exception;
}
