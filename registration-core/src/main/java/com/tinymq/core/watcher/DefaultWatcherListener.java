package com.tinymq.core.watcher;

import com.tinymq.common.dto.WatcherDto;
import com.tinymq.common.protocol.RequestCode;
import com.tinymq.core.status.StateMachine;
import com.tinymq.remote.exception.RemotingConnectException;
import com.tinymq.remote.exception.RemotingSendRequestException;
import com.tinymq.remote.exception.RemotingTimeoutException;
import com.tinymq.remote.exception.RemotingTooMuchException;
import com.tinymq.remote.netty.NettyRemotingClient;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultWatcherListener implements WatcherListener {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultWatcherListener.class);

    private final NettyRemotingClient client;
    private final StateMachine stateMachine;

    private final ExecutorService listenerPool;

    public DefaultWatcherListener(NettyRemotingClient client, final StateMachine stateMachine) {
        this.client = client;
        this.stateMachine = stateMachine;

        this.listenerPool = Executors.newFixedThreadPool(10,
                new ThreadFactory() {
            private final AtomicInteger threadIdx = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("[Watcher-Listener-%d]", threadIdx.getAndIncrement()));
            }
        });
    }

    @Override
    public void onKeyUpdate(String key, String oldVal, String newVal) {
        final Set<Watcher> watchers = this.stateMachine.getWatchers(key);
        if(watchers.isEmpty()) {
            return ;
        }
        LOG.debug("watcher listener listened: key [{}] update", key);

        for(Watcher w: watchers) {
            this.listenerPool.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.debug("watcher send update msg to [{}]", w.getClientAddr());
                    DefaultWatcherListener.this.sendOnewayMessage(w.getClientAddr(),
                            WatcherDto.create(null, key, oldVal, newVal));
                }
            });
        }
    }



    @Override
    public void onKeyDel(String key, String lastVal) {
        final Set<Watcher> watchers = this.stateMachine.getWatchers(key);
        if(watchers.isEmpty()) {
            return ;
        }
        LOG.debug("watcher listener listened: key [{}] update", key);

        for(Watcher w: watchers) {
            this.listenerPool.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.debug("watcher send del msg to [{}]", w.getClientAddr());
                    DefaultWatcherListener.this.sendOnewayMessage(w.getClientAddr(),
                            WatcherDto.create(null, key, lastVal, null));
                }
            });
        }
    }

    private void sendOnewayMessage(String addr, WatcherDto watcherDto) {

        RemotingCommand req = RemotingCommand.createRequest(RequestCode.REGISTRATION_CLIENT_WATCHER_RESPONSE);
        req.setBody(
                JSONSerializer.encode(watcherDto)
        );

        try {
            this.client.invokeOneway(addr, req, 1000);
        } catch (RemotingSendRequestException | RemotingConnectException | RemotingTimeoutException |
                 InterruptedException | RemotingTooMuchException e) {
            LOG.error("Exception occurred when in sending message in key [{}]", watcherDto.getWatcherKey(), e);
        }
    }
}
