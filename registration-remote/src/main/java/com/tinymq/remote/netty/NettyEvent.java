package com.tinymq.remote.netty;

import io.netty.channel.Channel;

public class NettyEvent {
    private String remoteAddr;
    private Channel channel;

    private NettyEventType eventType;

    public NettyEvent(String remoteAddr, Channel channel, NettyEventType eventType) {
        this.remoteAddr = remoteAddr;
        this.channel = channel;
        this.eventType = eventType;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public NettyEventType getEventType() {
        return eventType;
    }

    public void setEventType(NettyEventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "NettyEvent{" +
                "remoteAddr='" + remoteAddr + '\'' +
                ", channel=" + channel +
                ", eventType=" + eventType +
                '}';
    }
}
