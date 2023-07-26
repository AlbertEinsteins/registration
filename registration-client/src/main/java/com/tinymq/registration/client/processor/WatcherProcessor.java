package com.tinymq.registration.client.processor;

import cn.hutool.core.lang.Assert;
import com.tinymq.common.dto.WatcherDto;
import com.tinymq.registration.client.InvokeCallback;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.protocol.JSONSerializer;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.core.config.plugins.util.ResolverUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class WatcherProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(WatcherProcessor.class);

    private final ConcurrentHashMap<String, InvokeCallback> keyCallbackMap;

    public WatcherProcessor(final ConcurrentHashMap<String, InvokeCallback> callBackMap) {
        this.keyCallbackMap = callBackMap;
    }

    @Override
    public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
        // parse and exec callback
        WatcherDto resp;
        try {
            resp = JSONSerializer.decode(request.getBody(), WatcherDto.class);
        } catch (Exception e) {
            LOG.error("watcher decode exception", e);
            return null;
        }

        String key = resp.getWatcherKey();
        InvokeCallback callback = keyCallbackMap.get(key);
        Assert.notNull(callback, String.format("Key [%s] callback not exists.", key));

        try {
            callback.onKeyChanged(key, resp.getOldVal(), resp.getNewVal());
        } catch (Exception e) {
            LOG.error("exception occurred when execute callback", e);
        }
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
