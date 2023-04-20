package com.tinymq;

import com.tinymq.common.protocol.RequestCode;
import com.tinymq.core.DefaultRegistraionImpl;
import com.tinymq.core.Registration;
import com.tinymq.core.RegistrationConfig;
import com.tinymq.core.dto.outer.StateModel;
import com.tinymq.remote.exception.RemotingConnectException;
import com.tinymq.remote.exception.RemotingSendRequestException;
import com.tinymq.remote.exception.RemotingTimeoutException;
import com.tinymq.remote.exception.RemotingTooMuchException;
import com.tinymq.remote.netty.NettyClientConfig;
import com.tinymq.remote.netty.NettyRemotingClient;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class AppendLogTest {
    private Registration r1;
    private Registration r2;

    private NettyRemotingClient client = new NettyRemotingClient(new NettyClientConfig());
    @Test
    public void createNode1() throws InterruptedException {
        RegistrationConfig registrationConfig1 = new RegistrationConfig();
        registrationConfig1.setListenPort(7800);
        registrationConfig1.setSavePath(RegistrationConfig.DEFAULT_COMMIT_LOG_HOME + File.separator +
                registrationConfig1.getListenPort() + "store");

        r1 = new DefaultRegistraionImpl(registrationConfig1);
        r1.start();

        TimeUnit.SECONDS.sleep(1000);
    }

    @Test
    public void createNode2() throws InterruptedException {
        RegistrationConfig registrationConfig2 = new RegistrationConfig();
        registrationConfig2.setListenPort(7801);
        registrationConfig2.setSavePath(RegistrationConfig.DEFAULT_COMMIT_LOG_HOME + File.separator +
                registrationConfig2.getListenPort() + "store");

        r2 = new DefaultRegistraionImpl(registrationConfig2);
        r2.start();
        TimeUnit.SECONDS.sleep(1000);
    }

    @Test
    public void append() throws InterruptedException, RemotingSendRequestException, RemotingConnectException, RemotingTimeoutException, RemotingTooMuchException {
        client.start();

        RemotingCommand req = RemotingCommand.createRequest(RequestCode.REGISTRATION_CLIENT_READ);
//        req.setBody();
        StateModel<String, String> a = new StateModel<String, String>() {
            @Override
            public String getKey() {
                return "123";
            }

            @Override
            public String getVal(String key) {
                return "456";
            }
        };

        req.setBody(
                JSONSerializer.encode(a)
        );

        Thread.sleep(10);
        client.invokeOneway("127.0.0.1:7800", req, 1000);


        TimeUnit.SECONDS.sleep(1000);

    }

}
