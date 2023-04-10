package com.tinymq.remote.netty;

import com.tinymq.remote.common.RemotingUtils;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyDecoder extends LengthFieldBasedFrameDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyDecoder.class);
    private static final int MAX_FRAME_LENGTH = 16777216;

    public NettyDecoder() {
        super(MAX_FRAME_LENGTH, 0, 4, -4, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if(frame == null) {
                return null;
            }
            return RemotingCommand.decode(frame);
        } catch (Exception e) {
            LOGGER.error("decode exception " + RemotingUtils.parseRemoteAddress(ctx.channel()), e);
            ctx.close();
        } finally {
            if(frame != null) {
                frame.release();
            }
        }
        return null;
    }
}
