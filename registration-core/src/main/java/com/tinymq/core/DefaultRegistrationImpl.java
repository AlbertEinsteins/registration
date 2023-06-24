package com.tinymq.core;

import com.tinymq.core.config.RegistrationConfig;
import com.tinymq.core.status.KVStateMachine;
import com.tinymq.core.status.NodeManager;
import com.tinymq.core.status.StateMachine;
import com.tinymq.core.store.StoreManager;

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
