package com.tinymq.remote.netty;

public class NettySystemConfig {
    public static String REMOTING_CLIENT_WORKER_SIZE_VALUE = "com.tinymq.remote.client.worker.size";

    public static String REMOTING_CLIENT_SEMAPHORE_ONEWAY = "com.tinymq.remote.client.semaphore.oneway";

    public static String REMOTING_CLIENT_SEMAPHORE_ASYNC = "com.tinymq.remote.client.semaphore.async";

    public static String REMOTING_CLIENT_IDLE_SECONDS= "com.tinymq.remote.client.idle.milliseconds";
    public static int clientWorkerSize =
            Integer.parseInt(System.getProperty(REMOTING_CLIENT_WORKER_SIZE_VALUE, "4"));
    public static int semaphoreOneway =
            Integer.parseInt(System.getProperty(REMOTING_CLIENT_SEMAPHORE_ONEWAY, "32767"));
    public static int semaphoreAsync =
            Integer.parseInt(System.getProperty(REMOTING_CLIENT_SEMAPHORE_ASYNC, "32767"));

    public static int idleMilliseconds =
            Integer.parseInt(System.getProperty(REMOTING_CLIENT_IDLE_SECONDS, "120000"));

}
