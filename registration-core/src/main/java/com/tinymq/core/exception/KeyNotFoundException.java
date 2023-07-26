package com.tinymq.core.exception;

public class KeyNotFoundException extends RuntimeException {
    private String key;
    private String msg;

    public KeyNotFoundException(String key) {
        this.key = key;
    }
    public KeyNotFoundException(String key, String msg) {
        super(msg);
        this.key = key;
    }

    @Override
    public String toString() {
        return "KeyNotFoundException{" +
                "key='" + key + '\'' +
                ", msg='" + msg + '\'' +
                '}';
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
