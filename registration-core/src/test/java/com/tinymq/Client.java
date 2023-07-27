package com.tinymq;

import com.tinymq.common.protocol.ExtFieldDict;
import com.tinymq.common.protocol.RequestCode;
import com.tinymq.common.protocol.RequestStatus;
import com.tinymq.core.status.DefaultKVStateModel;
import com.tinymq.core.status.KVStateModel;
import com.tinymq.remote.RemotingClient;
import com.tinymq.remote.exception.RemotingConnectException;
import com.tinymq.remote.exception.RemotingSendRequestException;
import com.tinymq.remote.exception.RemotingTimeoutException;
import com.tinymq.remote.netty.NettyClientConfig;
import com.tinymq.remote.netty.NettyRemotingClient;
import com.tinymq.remote.protocol.RemotingCommand;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Client {
    private RemotingClient remotingClient;
    public Client() {
        this.remotingClient = new NettyRemotingClient(new NettyClientConfig());
        this.remotingClient.start();
    }
    public void send(String key, String val) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        RemotingCommand putReq = RemotingCommand.createRequest(RequestCode.REGISTRATION_CLIENT_WRITE);

        KVStateModel kvStateModel = new DefaultKVStateModel(key, val);
        putReq.setBody(
                kvStateModel.encode(kvStateModel)
        );

        KVStateModel restore = kvStateModel.decode(putReq.getBody());


        final RemotingCommand resp = this.remotingClient.invokeSync("127.0.0.1:7800", putReq, 10000);
        System.out.println(resp);
    }


    public void receive(String key) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        RemotingCommand getReq = RemotingCommand.createRequest(RequestCode.REGISTRATION_CLIENT_READ);

        getReq.setBody(
                key.getBytes(StandardCharsets.UTF_8)
        );

        RemotingCommand resp = this.remotingClient.invokeSync("127.0.0.1:7800", getReq, 10000);
        System.out.println(resp);
        System.out.println(new String(resp.getBody(), StandardCharsets.UTF_8));

    }
    public void shutdown() {
        this.remotingClient.shutdown();
    }

    public static void main(String[] args) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        Client client = new Client();

        try {
//            client.send("time1", "xxxxx");
//            client.send("time2", "22222");
//            client.send("time3", "33333");
//            client.send("time4", "44444");
//            client.send("time5", "55555");
//            client.receive("time1");
//            client.receive("time2");
//            client.receive("time3");
            client.receive("time4");
        } catch (Exception e) {
            e.printStackTrace();
        }


        client.shutdown();
    }



}
