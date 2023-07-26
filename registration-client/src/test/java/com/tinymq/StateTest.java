package com.tinymq;

import com.tinymq.core.status.DefaultKVStateModel;
import com.tinymq.core.status.KVStateModel;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class StateTest {

    @Test
    public void getIP() throws UnknownHostException {
        String ip = Inet4Address.getLocalHost().getHostAddress();
        System.out.println(ip);
    }
    @Test
    public void test() {
        KVStateModel kvStateModel = new DefaultKVStateModel();
        KVStateModel template = new DefaultKVStateModel();

        kvStateModel.setKey("123");
        kvStateModel.setVal("xxx");

        byte[] encoded = kvStateModel.encode(kvStateModel);
        KVStateModel stateModel = template.decode(encoded);
        System.out.println(stateModel);
    }

}
