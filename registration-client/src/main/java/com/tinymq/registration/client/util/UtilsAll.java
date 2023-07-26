package com.tinymq.registration.client.util;

import java.net.InetAddress;

public class UtilsAll {

    public static String getHostIP() {
        String ip = null;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ip;
    }
}
