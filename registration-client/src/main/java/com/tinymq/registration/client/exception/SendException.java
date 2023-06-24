package com.tinymq.registration.client.exception;


import com.tinymq.registration.client.dto.SendResult;

public class SendException extends Exception {

    private String msg;
    private Exception e;


    public SendException(String msg, Exception e) {
        this.msg = msg;
        this.e = e;
    }
}
