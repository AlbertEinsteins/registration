package com.tinymq.remote.netty;

public class NettyClientConfig {
    private int clientWorkerThreads = NettySystemConfig.clientWorkerSize;
    private int clientCallbackExecutorThreads = Runtime.getRuntime().availableProcessors();
    private int clientOnewaySemaphoreValue = NettySystemConfig.semaphoreOneway;
    private int clientAsyncSemaphoreValue = NettySystemConfig.semaphoreAsync;

    private int idleMilliseconds = NettySystemConfig.idleMilliseconds;



    public int getClientWorkerThreads() {
        return clientWorkerThreads;
    }

    public void setClientWorkerThreads(int clientWorkerThreads) {
        this.clientWorkerThreads = clientWorkerThreads;
    }

    public int getClientCallbackExecutorThreads() {
        return clientCallbackExecutorThreads;
    }

    public void setClientCallbackExecutorThreads(int clientCallbackExecutorThreads) {
        this.clientCallbackExecutorThreads = clientCallbackExecutorThreads;
    }

    public int getClientOnewaySemaphoreValue() {
        return clientOnewaySemaphoreValue;
    }

    public void setClientOnewaySemaphoreValue(int clientOnewaySemaphoreValue) {
        this.clientOnewaySemaphoreValue = clientOnewaySemaphoreValue;
    }

    public int getClientAsyncSemaphoreValue() {
        return clientAsyncSemaphoreValue;
    }

    public void setClientAsyncSemaphoreValue(int clientAsyncSemaphoreValue) {
        this.clientAsyncSemaphoreValue = clientAsyncSemaphoreValue;
    }

    public int getIdleMilliseconds() {
        return idleMilliseconds;
    }

    public void setIdleMilliseconds(int idleMilliseconds) {
        this.idleMilliseconds = idleMilliseconds;
    }
}
