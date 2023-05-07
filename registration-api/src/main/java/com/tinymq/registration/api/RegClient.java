package com.tinymq.registration.api;

import com.tinymq.core.dto.outer.StateModel;
import com.tinymq.registration.api.dto.KVStateModel;
import com.tinymq.remote.netty.NettyClientConfig;
import com.tinymq.remote.netty.NettyRemotingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RegClient implements PushRequest, PollRequest {
    private static final Logger LOG = LoggerFactory.getLogger(RegClient.class);
    private static final int maxCoreSize = 1 << 8;

    private final NettyRemotingClient client;
    private final NettyClientConfig clientConfig;

    private final ExecutorService publicExecutor;

    public RegClient(final NettyClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.client = new NettyRemotingClient(clientConfig);

        this.publicExecutor = new ThreadPoolExecutor(1, maxCoreSize, 60, TimeUnit.SECONDS,
                new SynchronousQueue<>(), new ThreadFactory() {
            private final AtomicInteger threadIdx = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("[RegClient-PublicThreadPool-%d]", threadIdx.getAndIncrement()));
            }
        });
    }

    public void shutdown() {
        this.publicExecutor.shutdown();
    }


    @Override
    public PushResult getByKey(Object key) {
        PushResult pushResult = new PushResult();


        return null;
    }

    @Override
    public PollResult getByKey(StateModel key) {


        return null;
    }
}
