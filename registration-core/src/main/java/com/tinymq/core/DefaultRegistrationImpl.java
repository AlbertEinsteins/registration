package com.tinymq.core;

import com.tinymq.core.config.RegistrationConfig;
import com.tinymq.core.status.KVStateMachine;
import com.tinymq.core.status.NodeManager;
import com.tinymq.core.status.StateMachine;
import com.tinymq.core.store.StoreManager;
import com.tinymq.core.watcher.DefaultWatcherListener;
import com.tinymq.core.watcher.WatcherListener;

public class DefaultRegistrationImpl implements Registration {

    private final NodeManager nodeManager;
    private final RegistrationConfig registrationConfig;

    private final StoreManager storeManager;

    private final StateMachine stateMachine;


    public DefaultRegistrationImpl(final RegistrationConfig registrationConfig) {
        this.registrationConfig = registrationConfig;

        this.stateMachine = new KVStateMachine();

        this.storeManager = new StoreManager(registrationConfig, this.stateMachine);
        this.nodeManager = new NodeManager(registrationConfig, storeManager, stateMachine);

        // 注入 key update listener
        WatcherListener watcherListener = new DefaultWatcherListener(
                nodeManager.getNettyRemotingClient(), stateMachine);
        this.stateMachine.registryWatcherListener(watcherListener);
    }

    @Override
    public void start() {
        this.nodeManager.start();
    }

    @Override
    public void shutdown() {
        this.storeManager.shutdown();
        this.nodeManager.shutdown();
    }
}
