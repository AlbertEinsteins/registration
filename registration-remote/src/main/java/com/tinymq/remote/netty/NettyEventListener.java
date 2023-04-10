package com.tinymq.remote.netty;

import io.netty.channel.Channel;

/**
 * 由服务线程处理Netty通信中的各种事件
 */
public interface NettyEventListener {

    void onChannelConnect(final String remoteAddr, final Channel channel);
    void onChannelClose(final String remoteAddr, final Channel channel);
    void onChannelIdle(final String remoteAddr, final Channel channel);
    void onChannelException(final String remoteAddr, final Channel channel);
}
