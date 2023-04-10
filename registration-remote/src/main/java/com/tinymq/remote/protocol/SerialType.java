package com.tinymq.remote.protocol;

public enum SerialType {
    JSON((byte)0),
    CUSTOME((byte)1);

    private final byte code;
    SerialType(byte code) {
        this.code = code;
    }
    public static SerialType fromKey(byte code) {
        for(SerialType serialType : SerialType.values()) {
            if(serialType.code == code) {
                return serialType;
            }
        }
        return JSON;
    }

    public byte getCode() {
        return code;
    }
}
