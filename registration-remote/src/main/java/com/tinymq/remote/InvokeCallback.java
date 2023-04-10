package com.tinymq.remote;


import com.tinymq.remote.netty.ResponseFuture;

public interface InvokeCallback {
    void operationComplete(ResponseFuture responseFuture);
}
