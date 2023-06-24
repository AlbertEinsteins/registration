package com.tinymq.registration.client;

import com.tinymq.registration.client.dto.SendRequest;
import com.tinymq.registration.client.dto.SendResult;
import com.tinymq.registration.client.exception.SendException;
import com.tinymq.registration.client.rpc.DefaultDelegateRPC;
import com.tinymq.registration.client.rpc.DelegateRPC;
import com.tinymq.remote.netty.NettyClientConfig;
import com.tinymq.remote.netty.NettyRemotingClient;
import com.tinymq.remote.protocol.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;

public class KVRegClient implements RegClient {
    private static final Logger LOG = LoggerFactory.getLogger(KVRegClient.class);

    private final NettyRemotingClient nettyRemotingClient;
    private final NettyClientConfig nettyClientConfig;

    /*store server nodes*/
    private final CopyOnWriteArraySet<String /*addr <ip:port>*/> serverNodes = new CopyOnWriteArraySet<>();

    private volatile String leaderAddr;
    private final DelegateRPC delegateRPC;

    public KVRegClient() {
        this.nettyClientConfig = new NettyClientConfig();
        this.nettyRemotingClient = new NettyRemotingClient(this.nettyClientConfig);
        this.delegateRPC = new DefaultDelegateRPC(nettyRemotingClient);
    }

    public void start() {
        this.nettyRemotingClient.start();
    }
    public void shutdown() {
        this.nettyRemotingClient.shutdown();
    }

    public KVRegClient(final NettyClientConfig clientConfig) {
        this.nettyClientConfig = clientConfig;
        this.nettyRemotingClient = new NettyRemotingClient(clientConfig);
        this.delegateRPC = new DefaultDelegateRPC(nettyRemotingClient);
    }

    public void addNode(String nodeUrl) {
        this.serverNodes.add(nodeUrl);
    }
    public void addNodes(String... urls) {
        this.serverNodes.addAll(Arrays.asList(urls));
    }


    private String selectLeader() {
        if(serverNodes.isEmpty()) {
            System.out.println("Must initialized nodes first...");
            return null;
        }

        if(leaderAddr == null) {
            leaderAddr = serverNodes.iterator().next();
        }
        return leaderAddr;
    }


    @Override
    public SendResult get(String key, long timeoutMillis) throws SendException {
        String leaderAddr = selectLeader();
        SendRequest req = SendRequest.createPollReq(leaderAddr, key);
        final SendResult result = delegateRPC.send(req, timeoutMillis, null);

        switch (result.getStatus()) {
            case READ_SUCCESS:
                return result;
            case EXCEPTION_OCCURRED:
                throw new SendException("exception occurred.", null);
            case NOT_LEADER:
                this.leaderAddr = JSONSerializer.decode(result.getResult(), String.class);
                return delegateRPC.send(req, timeoutMillis, null);
        }
        return null;
    }

    @Override
    public SendResult put(String key, String val, long timeoutMillis) throws SendException {
            String leaderAddr = selectLeader();
        final SendRequest pushReq = SendRequest.createPushReq(leaderAddr, key, val);
        final SendResult result = delegateRPC.send(pushReq,
                timeoutMillis, null);

        switch (result.getStatus()) {
            case WRITE_SUCCESS:
                return result;
            case EXCEPTION_OCCURRED:
                throw new SendException("exception occurred.", null);
            case NOT_LEADER:
                this.leaderAddr = JSONSerializer.decode(result.getResult(), String.class);
                return delegateRPC.send(pushReq, timeoutMillis, null);
        }
        return null;
    }

}
