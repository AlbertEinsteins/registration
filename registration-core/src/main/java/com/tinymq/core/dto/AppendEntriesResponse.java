package com.tinymq.core.dto;

public class AppendEntriesResponse {
    private int currentTerm;
    private boolean isSuccess;

    private int matchIndex;

    public static AppendEntriesResponse create(int currentTerm, boolean isSuccess, int matchIndex) {
        AppendEntriesResponse response = new AppendEntriesResponse();
        response.setCurrentTerm(currentTerm);
        response.setSuccess(isSuccess);
        response.setMatchIndex(matchIndex);
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

    public int getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(int matchIndex) {
        this.matchIndex = matchIndex;
    }
}
