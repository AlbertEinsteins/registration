package com.tinymq.remote;

import com.tinymq.remote.protocol.RemotingCommand;

public interface RPCHook {
    /**
     * 发送前执行前执行的操作
     */
    void doBefore(final String addr, final RemotingCommand request);
    /**
     * 发送前执行后执行的操作
     */
    void doAfter(final String addr, final RemotingCommand request, final RemotingCommand response);
}
