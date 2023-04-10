package com.tinymq.remote.netty;

public class NettyServerConfig {

    private int listenPort = 7800;
    private int serverWorkerThreads = NettySystemConfig.clientWorkerSize;
    private int serverCallbackExecutorThreads = Runtime.getRuntime().availableProcessors();
    private int serverOnewaySemaphoreValue = NettySystemConfig.semaphoreOneway;
    private int serverAsyncSemaphoreValue = NettySystemConfig.semaphoreAsync;

    private int idleMilliSeconds = NettySystemConfig.idleMilliseconds;


    public int getServerWorkerThreads() {
        return serverWorkerThreads;
    }

    public void setServerWorkerThreads(int serverWorkerThreads) {
        this.serverWorkerThreads = serverWorkerThreads;
    }

    public int getServerCallbackExecutorThreads() {
        return serverCallbackExecutorThreads;
    }

    public void setServerCallbackExecutorThreads(int serverCallbackExecutorThreads) {
        this.serverCallbackExecutorThreads = serverCallbackExecutorThreads;
    }

    public int getServerOnewaySemaphoreValue() {
        return serverOnewaySemaphoreValue;
    }

    public void setServerOnewaySemaphoreValue(int serverOnewaySemaphoreValue) {
        this.serverOnewaySemaphoreValue = serverOnewaySemaphoreValue;
    }

    public int getServerAsyncSemaphoreValue() {
        return serverAsyncSemaphoreValue;
    }

    public void setServerAsyncSemaphoreValue(int serverAsyncSemaphoreValue) {
        this.serverAsyncSemaphoreValue = serverAsyncSemaphoreValue;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getIdleMilliSeconds() {
        return idleMilliSeconds;
    }

    public void setIdleMilliSeconds(int idleMilliSeconds) {
        this.idleMilliSeconds = idleMilliSeconds;
    }
}
