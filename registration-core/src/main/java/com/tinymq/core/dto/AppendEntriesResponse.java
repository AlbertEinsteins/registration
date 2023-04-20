package com.tinymq.core.dto;

public class AppendEntriesResponse {
    private int currentTerm;
    private boolean isSuccess;

    public static AppendEntriesResponse create(int currentTerm, boolean isSuccess) {
        AppendEntriesResponse response = new AppendEntriesResponse();
        response.setCurrentTerm(currentTerm);
        response.setSuccess(isSuccess);
        return response;
    }
    public int getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }
}
