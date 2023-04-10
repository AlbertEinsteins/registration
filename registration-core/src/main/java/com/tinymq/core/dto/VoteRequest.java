package com.tinymq.core.dto;

public class VoteRequest {
    private int term;           // 当前任期

    private String candidateAddr;    // 被投票人

    private int lastLogIndex;   // 上次log entry 的下标
    private int lastLogTerm;    // 上次log entry 的任期


    public static VoteRequest createVote(int term, String candidateAddr, int lastLogIndex, int lastLogTerm) {
        return new VoteRequest(term, candidateAddr, lastLogIndex, lastLogTerm);
    }

    public VoteRequest(int term, String candidateAddr, int lastLogIndex, int lastLogTerm) {
        this.term = term;
        this.candidateAddr = candidateAddr;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public String getCandidateAddr() {
        return candidateAddr;
    }

    public void setCandidateAddr(String candidateAddr) {
        this.candidateAddr = candidateAddr;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogIndex(int lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    public void setLastLogTerm(int lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString() {
        return "VoteRequest{" +
                "term=" + term +
                ", candidateAddr='" + candidateAddr + '\'' +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }
}
