package com.tinymq;

import com.tinymq.core.DefaultRegistrationImpl;
import com.tinymq.core.Registration;
import com.tinymq.core.config.RegistrationConfig;

public class Server3 {
    static Registration node(String configPath) {
        RegistrationConfig registrationConfig = new RegistrationConfig(configPath);
        return new DefaultRegistrationImpl(registrationConfig);
    }

    public static void main(String[] args) throws InterruptedException {
        node("registration3.yaml").start();
    }
}
