package com.tinymq.core.status;

import com.tinymq.remote.protocol.JSONSerializer;

import java.nio.charset.StandardCharsets;

public class DefaultKVStateModel implements KVStateModel {
    private String key;
    private String val;
    public DefaultKVStateModel() {
    }

    public DefaultKVStateModel(String key, String val) {
        this.key = key;
        this.val = val;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void setVal(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }

    @Override
    public String getKey() {
        return key;
    }


    @Override
    public KVStateModel decode(byte[] bytes) {
        return JSONSerializer.decode(bytes, this.getClass());
    }

    @Override
    public byte[] encode(KVStateModel kvStateModel) {
        return JSONSerializer.toJson(kvStateModel).getBytes(StandardCharsets.UTF_8);
    }
}
