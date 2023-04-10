package remote;



import com.tinymq.remote.exception.RemotingConnectException;
import com.tinymq.remote.exception.RemotingSendRequestException;
import com.tinymq.remote.exception.RemotingTimeoutException;
import com.tinymq.remote.exception.RemotingTooMuchException;
import com.tinymq.remote.netty.NettyClientConfig;
import com.tinymq.remote.netty.NettyEventListener;
import com.tinymq.remote.netty.NettyRemotingClient;
import com.tinymq.remote.protocol.RemotingCommand;
import io.netty.channel.Channel;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class Client {

    static class MyNettyHandlerListener implements NettyEventListener {
        @Override
        public void onChannelConnect(String remoteAddr, Channel channel) {

        }

        @Override
        public void onChannelClose(String remoteAddr, Channel channel) {

        }

        @Override
        public void onChannelIdle(String remoteAddr, Channel channel) {
            System.out.println("channel idle");
        }

        @Override
        public void onChannelException(String remoteAddr, Channel channel) {

        }
    }

    public static void main(String[] args) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, RemotingTooMuchException {
        NettyClientConfig clientConfig = new NettyClientConfig();
        NettyRemotingClient client = new NettyRemotingClient(clientConfig, new MyNettyHandlerListener());

        client.start();

        //create message
        RemotingCommand msg = new RemotingCommand();
        String body = "123";
        msg.setBody(body.getBytes(StandardCharsets.UTF_8));

        // set request code
        msg.setCode(100);

//        client.invokeOneway("127.0.0.1:7800", msg, 10000);
        RemotingCommand remotingCommand = client.invokeSync("127.0.0.1:7800", msg, 500);
        System.out.println(new String(remotingCommand.getBody()));
    }
}
