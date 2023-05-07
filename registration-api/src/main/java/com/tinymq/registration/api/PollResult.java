package com.tinymq.registration.api;

import com.tinymq.core.dto.outer.StateModel;

public class PollResult {
    private String info;
    private StateModel result;
    private boolean success;

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public StateModel getResult() {
        return result;
    }

    public void setResult(StateModel result) {
        this.result = result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
