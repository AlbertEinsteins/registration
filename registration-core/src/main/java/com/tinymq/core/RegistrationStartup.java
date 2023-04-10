package com.tinymq.core;

import com.tinymq.core.DefaultRegistraionImpl;
import com.tinymq.core.Registration;

public class RegistrationStartup {

    public static void main(String[] args) {
        main0();
    }

    private static void main0() {
        Registration registration = new DefaultRegistraionImpl();
        registration.start();
    }
}
