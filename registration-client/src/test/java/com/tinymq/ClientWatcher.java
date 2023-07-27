package com.tinymq;

import com.tinymq.registration.client.InvokeCallback;
import com.tinymq.registration.client.KVRegClient;
import com.tinymq.registration.client.RegClient;
import com.tinymq.registration.client.dto.SendResult;

import java.util.concurrent.TimeUnit;

public class ClientWatcher {
    public static void main(String[] args) throws InterruptedException {
        RegClient regClient = new KVRegClient(8100);
        regClient.addServerNodes("127.0.0.1:7800", "127.0.0.1:7801", "127.0.0.1:7802");
        regClient.start();

        SendResult res = regClient.createNode("time2");
        if(!res.isSuccess()) {
            System.out.println("create node err: " + res.getInfo());
            return ;
        }

        res = regClient.addWatcher("time2", new InvokeCallback() {
            @Override
            public void onKeyChanged(String key, String oldV, String newV) throws Exception {
                System.out.println(key);
                System.out.println(oldV);
                System.out.println(newV);
            }
        });

        if(!res.isSuccess()) {
            System.out.println(res.getInfo());
        }


        TimeUnit.SECONDS.sleep(300);

        regClient.shutdown();
    }
}
