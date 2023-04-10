package com.tinymq.core;

import com.tinymq.core.status.NodeManager;
import com.tinymq.core.store.StoreManager;

public class DefaultRegistraionImpl implements Registration {

    private final NodeManager nodeManager;
    private final RegistrationConfig registrationConfig;
    private final StoreManager storeManager;

    public DefaultRegistraionImpl() {
        this.registrationConfig = resolveConfigFile();

        this.nodeManager = new NodeManager(registrationConfig);
        this.storeManager = new StoreManager();
    }

    private RegistrationConfig resolveConfigFile() {
        return new RegistrationConfig()
                .setListenPort(7800);
    }

    @Override
    public void start() {
        this.nodeManager.start();

    }

    @Override
    public void shutdown() {

    }
}
