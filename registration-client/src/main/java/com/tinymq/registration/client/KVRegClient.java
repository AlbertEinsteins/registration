package com.tinymq.registration.client;

import cn.hutool.core.lang.Assert;
import com.tinymq.common.dto.WatcherDto;
import com.tinymq.common.protocol.RequestCode;
import com.tinymq.common.protocol.RequestStatus;
import com.tinymq.core.status.DefaultKVStateModel;
import com.tinymq.core.status.KVStateModel;
import com.tinymq.registration.client.dto.SendRequest;
import com.tinymq.registration.client.dto.SendResult;
import com.tinymq.registration.client.exception.SendException;
import com.tinymq.registration.client.processor.WatcherProcessor;
import com.tinymq.registration.client.rpc.DefaultDelegateRPC;
import com.tinymq.registration.client.rpc.DelegateRPC;
import com.tinymq.registration.client.util.UtilsAll;
import com.tinymq.remote.netty.NettyClientConfig;
import com.tinymq.remote.netty.NettyRemotingClient;
import com.tinymq.remote.netty.NettyRemotingServer;
import com.tinymq.remote.netty.NettyServerConfig;
import com.tinymq.remote.protocol.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class KVRegClient implements RegClient {
    private static final Logger LOG = LoggerFactory.getLogger(KVRegClient.class);
    private static final long DEFAULT_TIMEOUTMILLIS = 2000;

    private final NettyRemotingClient nettyRemotingClient;
    private final NettyClientConfig nettyClientConfig;

    private final NettyServerConfig nettyServerConfig;
    private final NettyRemotingServer nettyRemotingServer;

    private final WatcherProcessor watcherProcessor;

    /*store server nodes*/
    private final CopyOnWriteArraySet<String /*addr <ip:port>*/> serverNodes = new CopyOnWriteArraySet<>();

    private volatile String leaderAddr;
    private final DelegateRPC delegateRPC;

    // 各个Key的回调map
    private final ConcurrentHashMap<String /*Key*/, InvokeCallback> callBackMap = new ConcurrentHashMap<>();

    private long timeoutMillis = DEFAULT_TIMEOUTMILLIS;

    public KVRegClient(int listenPort) {
        this.nettyClientConfig = new NettyClientConfig();
        this.nettyRemotingClient = new NettyRemotingClient(this.nettyClientConfig);
        this.nettyServerConfig = new NettyServerConfig();
        this.nettyServerConfig.setListenPort(listenPort);
        this.nettyRemotingServer = new NettyRemotingServer(this.nettyServerConfig);
        this.delegateRPC = new DefaultDelegateRPC(nettyRemotingClient);

        this.watcherProcessor = new WatcherProcessor(callBackMap);
        this.registryProcessor();
    }

    private void registryProcessor() {
        nettyRemotingServer.registerProcessor(RequestCode.REGISTRATION_CLIENT_WATCHER_RESPONSE, watcherProcessor, null);
    }

    public void start() {
        this.nettyRemotingClient.start();
        this.nettyRemotingServer.start();
    }
    public void shutdown() {
        this.nettyRemotingClient.shutdown();
        this.nettyRemotingServer.shutdown();
    }


    public void addServerNodes(String... urls) {
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
    public SendResult get(String key) throws SendException {
        Assert.notNull(key, "The key could not be null");
        String leaderAddr = selectLeader();

        byte[] body = key.getBytes(
                StandardCharsets.UTF_8
        );
        SendRequest req = SendRequest.createPollReq(leaderAddr, RequestCode.REGISTRATION_CLIENT_READ, body);
        final SendResult result = delegateRPC.send(req, timeoutMillis);

        switch (result.getStatus()) {
            case READ_SUCCESS:
                return result;
            case EXCEPTION_OCCURRED:
                throw new SendException("exception occurred.", null);
            case NOT_LEADER:
                this.leaderAddr = new String(result.getResult(), StandardCharsets.UTF_8);
                req.setAddr(this.leaderAddr);
                return delegateRPC.send(req, timeoutMillis);
        }
        return null;
    }

    @Override
    public SendResult put(String key, String val) throws SendException {
        String leaderAddr = selectLeader();

        KVStateModel kvStateModel = new DefaultKVStateModel(key, val);
        byte[] body = kvStateModel.encode(kvStateModel);
        final SendRequest pushReq = SendRequest.createPushReq(leaderAddr, RequestCode.REGISTRATION_CLIENT_WRITE, body);
        final SendResult result = delegateRPC.send(pushReq, timeoutMillis);

        switch (result.getStatus()) {
            case WRITE_SUCCESS:
                return result;
            case EXCEPTION_OCCURRED:
                throw new SendException("exception occurred." + result.getInfo(), null);
            case NOT_LEADER:
                this.leaderAddr = new String(result.getResult(), StandardCharsets.UTF_8);
                pushReq.setAddr(this.leaderAddr);
                return delegateRPC.send(pushReq, timeoutMillis);
        }
        return null;
    }

    @Override
    public SendResult addWatcher(String key, InvokeCallback invokeCallback) {
        String leaderAddr = selectLeader();

        Assert.notNull(invokeCallback, "The key listener can not be null");
        String clientAddr = UtilsAll.getHostIP() + ":" + this.nettyServerConfig.getListenPort();

        WatcherDto watcherDto = WatcherDto.create(clientAddr, key);
        byte[] body = JSONSerializer.encode(watcherDto);
        SendRequest sendRequest = SendRequest.createPushReq(leaderAddr, RequestCode.REGISTRATION_CLIENT_WATCHER_ADD, body);
        SendResult result = null;
        try {
            result = delegateRPC.send(sendRequest, timeoutMillis);
        } catch (SendException e) {
            LOG.error("Exception occurred when in send watcher request", e);
            return SendResult.create(false, null, "exception occurred", RequestStatus.EXCEPTION_OCCURRED);
        }

        switch (result.getStatus()) {
            case WRITE_SUCCESS:
                LOG.debug("Add watcher [key: {}]", key);
                this.callBackMap.put(key, invokeCallback);
                return result;
            case KEY_NOT_EXIST:
            case EXCEPTION_OCCURRED:
                result.setSuccess(false);
                return result;

            case NOT_LEADER:
                this.leaderAddr = new String(result.getResult(), StandardCharsets.UTF_8);
                try {
                    return delegateRPC.send(sendRequest, timeoutMillis);
                } catch (SendException e) {
                    LOG.error("exception", e);
                }
                return null;
        }
        throw new RuntimeException("Not support error type");
    }

    @Override
    public SendResult createNode(String key) {
        String leaderAddr = selectLeader();

        WatcherDto watcherDto = WatcherDto.create(null, key);
        byte[] body = JSONSerializer.encode(watcherDto);
        SendRequest sendRequest = SendRequest.createPushReq(leaderAddr, RequestCode.REGISTRATION_CLIENT_KEY_CREATE, body);
        SendResult result = null;
        try {
            result = delegateRPC.send(sendRequest, timeoutMillis);
        } catch (SendException e) {
            LOG.error("Exception occurred when in send watcher request", e);
            return SendResult.create(false, null, "exception occurred", RequestStatus.EXCEPTION_OCCURRED);
        }

        switch (result.getStatus()) {
            case WRITE_SUCCESS:
                return result;
            case KEY_NOT_EXIST:
            case EXCEPTION_OCCURRED:
                // 操作失败
                result.setSuccess(false);
                return result;

            case NOT_LEADER:
                this.leaderAddr = new String(result.getResult(), StandardCharsets.UTF_8);
                try {
                    return delegateRPC.send(sendRequest, timeoutMillis);
                } catch (SendException e) {
                    LOG.error("exception", e);
                }
                return null;
        }
        throw new RuntimeException("Not support error type");
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
}
