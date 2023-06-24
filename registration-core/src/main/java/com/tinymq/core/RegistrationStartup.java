package com.tinymq.core;

import com.tinymq.core.config.RegistrationConfig;

public class RegistrationStartup {

    public static void main(String[] args) {
        main0();
    }

    private static void main0() {
        RegistrationConfig registrationConfig = new RegistrationConfig("registration.yaml");

        Registration registration = new DefaultRegistrationImpl(registrationConfig);
        registration.start();

        registration.shutdown();
    }
}
