package com.tinymq.remote;

public interface RemotingService {
    /**
     * 服务启动
     */
    void start();

    /**
     * 关闭服务
     */
    void shutdown();

    /**
     * 注册钩子函数
     * 在消息发送前后起作用
     * @param hook 钩子
     */
    void registerRPCHook(final RPCHook hook);
}
