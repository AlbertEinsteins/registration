package com.tinymq.core.dto;

public class VoteResponse {
    private int term;

    private boolean isVoteGranted;

    public VoteResponse() {}

    public static VoteResponse createVote(int term, boolean isVoteGranted) {
        VoteResponse resp = new VoteResponse();
        resp.setTerm(term);
        resp.setVoteGranted(isVoteGranted);
        return resp;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public VoteResponse(int term, boolean isVoteGranted) {
        this.term = term;
        this.isVoteGranted = isVoteGranted;
    }

    public boolean isVoteGranted() {
        return isVoteGranted;
    }

    public void setVoteGranted(boolean voteGranted) {
        isVoteGranted = voteGranted;
    }

    @Override
    public String toString() {
        return "VoteResposne{" +
                "term=" + term +
                ", isVoteGranted=" + isVoteGranted +
                '}';
    }
}
