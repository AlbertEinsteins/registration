package remote;

import com.tinymq.remote.netty.NettyEventListener;
import com.tinymq.remote.netty.NettyRemotingServer;
import com.tinymq.remote.netty.NettyServerConfig;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

public class Server {
    static class MyRequestProcessor implements RequestProcessor {
        @Override
        public RemotingCommand process(ChannelHandlerContext ctx, RemotingCommand request) {
            String receive = new String(request.getBody(), StandardCharsets.UTF_8);
            System.out.println(receive);

            RemotingCommand response = RemotingCommand.createResponse(request.getCode(), "success");
            response.setBody(request.getBody());
            return response;
        }

        @Override
        public boolean rejectRequest() {
            return false;
        }
    }
    static class MyNettyEventListener implements NettyEventListener {
        @Override
        public void onChannelConnect(String remoteAddr, Channel channel) {
            System.out.println("channel is connect");
        }

        @Override
        public void onChannelClose(String remoteAddr, Channel channel) {
            System.out.println("channel is close");

        }

        @Override
        public void onChannelIdle(String remoteAddr, Channel channel) {
            System.out.println("channel is idle " + remoteAddr);
        }

        @Override
        public void onChannelException(String remoteAddr, Channel channel) {
            System.out.println("channel exception");
        }
    }

    public static void main(String[] args) {
        NettyServerConfig serverConfig = new NettyServerConfig();
        serverConfig.setListenPort(7800);

        NettyRemotingServer server = new NettyRemotingServer(serverConfig, new MyNettyEventListener());
        server.registerProcessor(100, new MyRequestProcessor(), null);

        server.start();
    }
}
