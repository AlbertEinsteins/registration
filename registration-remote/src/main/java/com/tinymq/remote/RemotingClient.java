package com.tinymq.remote;


import com.tinymq.remote.exception.RemotingConnectException;
import com.tinymq.remote.exception.RemotingSendRequestException;
import com.tinymq.remote.exception.RemotingTimeoutException;
import com.tinymq.remote.exception.RemotingTooMuchException;
import com.tinymq.remote.netty.RequestProcessor;
import com.tinymq.remote.protocol.RemotingCommand;

import java.util.concurrent.ExecutorService;

public interface RemotingClient extends RemotingService {

    /**
     * 同步执行
     * @param addr
     * @param request
     * @param timeoutMillis
     * @return
     */
    RemotingCommand invokeSync(final String addr, final RemotingCommand request, long timeoutMillis)
            throws RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException, InterruptedException;

    /**
     * 异步执行
     * @param addr
     * @param request
     * @param timeoutMillis
     * @param callback
     */
    void invokeAsync(final String addr, final RemotingCommand request, long timeoutMillis,
                     InvokeCallback callback) throws RemotingConnectException, RemotingTimeoutException, InterruptedException, RemotingSendRequestException, RemotingTooMuchException;

    /**
     * 单向执行
     * @param addr
     * @param request
     * @param timeoutMillis
     */
    void invokeOneway(final String addr, final RemotingCommand request, long timeoutMillis) throws RemotingConnectException, InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingTooMuchException;

    void registerProcessor(final int code, final RequestProcessor processor, ExecutorService executorService);

    ExecutorService getCallbackExecutor();

    void setCallbackExecutor(final ExecutorService executorService);
}
