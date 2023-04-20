package com.tinymq.core;

import com.tinymq.core.status.NodeManager;
import com.tinymq.core.store.StoreManager;

public class DefaultRegistraionImpl implements Registration {

    private final NodeManager nodeManager;
    private final RegistrationConfig registrationConfig;

    private final StoreManager storeManager;

    public DefaultRegistraionImpl(final RegistrationConfig registrationConfig) {
        this.registrationConfig = registrationConfig;
        this.storeManager = new StoreManager(registrationConfig);
        this.nodeManager = new NodeManager(registrationConfig, storeManager);
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
