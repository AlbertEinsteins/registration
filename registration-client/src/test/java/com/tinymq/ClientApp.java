package com.tinymq;

import cn.hutool.json.JSONUtil;
import com.tinymq.registration.client.KVRegClient;
import com.tinymq.registration.client.dto.SendResult;
import com.tinymq.registration.client.exception.SendException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.StandardCharsets;

@RunWith(JUnit4.class)
public class ClientApp {
    private KVRegClient regClient;

    @Before
    public void testPutAndGet() {
        this.regClient = new KVRegClient(8101);
        regClient.addServerNodes("127.0.0.1:7800", "127.0.0.1:7801", "127.0.0.1:7802");
        regClient.start();

    }

    @Test
    public void putAndGet() throws SendException {
        final SendResult sendResult = regClient.put("time2", "xxxxx");
        if(sendResult.isSuccess()) {
            final SendResult result = regClient.get("time2");
            System.out.println(new String(result.getResult(), StandardCharsets.UTF_8));
        }
    }

    @After
    public void after() {
        this.regClient.shutdown();
    }
}
