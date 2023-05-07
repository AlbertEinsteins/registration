package com.tinymq.registration.api.dto;

import com.tinymq.core.dto.outer.StateModel;

public class KVStateModel implements StateModel<String, String> {
    private String key;
    private String value;

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public String getVal(String key) {
        return null;
    }
}
