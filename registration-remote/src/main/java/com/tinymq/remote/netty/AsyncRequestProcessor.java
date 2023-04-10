package com.tinymq.remote.netty;

import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;

public abstract class AsyncRequestProcessor implements RequestProcessor {
    public void asyncProcess(ChannelHandlerContext ctx, RemotingCommand request,
                                        ResponseCallBack responseCallBack) {
        RemotingCommand response = process(ctx, request);
        responseCallBack.operationComplete(response);
    }
}
