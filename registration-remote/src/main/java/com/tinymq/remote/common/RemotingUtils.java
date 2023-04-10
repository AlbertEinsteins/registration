package com.tinymq.remote.common;


import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class RemotingUtils {
    private RemotingUtils() { }

    public static String parseRemoteAddress(final Channel channel) {
        if(channel != null) {
            return parseRemoteAddress(channel.remoteAddress());
        }
        return "";
    }
    public static String parseRemoteAddress(final SocketAddress socketAddress) {
        String remoteAddress = socketAddress.toString();

        if(remoteAddress != null) {
            int split = remoteAddress.lastIndexOf("/");
            return remoteAddress.substring(split + 1);
        }
        return "";
    }


    public static InetSocketAddress addrToNetAddress(final String addr) {
        if(addr == null || addr.split(":").length != 2) {
            return null;
        }
        String ip = addr.split(":")[0];
        int port = Integer.parseInt(addr.split(":")[1]);
        return InetSocketAddress.createUnresolved(ip, port);
    }
}
