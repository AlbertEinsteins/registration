package com.tinymq.remote.netty;


import com.tinymq.remote.protocol.RemotingCommand;

public interface ResponseCallBack {
    void operationComplete(RemotingCommand response);
}
