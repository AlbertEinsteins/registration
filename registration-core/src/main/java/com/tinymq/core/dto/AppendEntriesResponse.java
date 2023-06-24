package com.tinymq.core.dto;

public class AppendEntriesResponse {
    private int currentTerm;
    private boolean isSuccess;

    //如果失败，携带失败原因
    private AppendEntriesFailedType failedType;

    private int matchIndex;

    public static AppendEntriesResponse create(int currentTerm, boolean isSuccess, int matchIndex,
                                               AppendEntriesFailedType failedType) {
        AppendEntriesResponse response = new AppendEntriesResponse();
        response.setCurrentTerm(currentTerm);
        response.setSuccess(isSuccess);
        response.setMatchIndex(matchIndex);
        response.setFailedType(failedType);
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

    public AppendEntriesFailedType getFailedType() {
        return failedType;
    }

    public void setFailedType(AppendEntriesFailedType failedType) {
        this.failedType = failedType;
    }
}
