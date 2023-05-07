package com.tinymq.core.dto;

import com.tinymq.core.store.CommitLogEntry;

import java.util.List;

public class AppendEntriesRequest {
    private int term;

    private String leaderAddr;

    private int prevLogIndex;

    private int prevLogTerm;

    private List<CommitLogEntry> commitLogEntries;

    private int leaderCommitIndex;

    public static AppendEntriesRequest create(int term, String leaderAddr, int prevLogIndex, int prevLogTerm, int leaderCommitIndex) {
        AppendEntriesRequest request = new AppendEntriesRequest();
        request.setTerm(term);
        request.setLeaderAddr(leaderAddr);
        request.setPrevLogIndex(prevLogIndex);
        request.setPrevLogTerm(prevLogTerm);
        request.setLeaderCommitIndex(leaderCommitIndex);
        return request;
    }
    public static AppendEntriesRequest create(int term, String leaderAddr, int prevLogIndex, int prevLogTerm, int leaderCommitIndex,
                                              List<CommitLogEntry> commitLogEntries) {
        AppendEntriesRequest request = new AppendEntriesRequest();
        request.setTerm(term);
        request.setLeaderAddr(leaderAddr);
        request.setPrevLogIndex(prevLogIndex);
        request.setPrevLogTerm(prevLogTerm);
        request.setLeaderCommitIndex(leaderCommitIndex);
        request.setCommitLogEntries(commitLogEntries);
        return request;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public String getLeaderAddr() {
        return leaderAddr;
    }

    public void setLeaderAddr(String leaderAddr) {
        this.leaderAddr = leaderAddr;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public List<CommitLogEntry> getCommitLogEntries() {
        return commitLogEntries;
    }

    public void setCommitLogEntries(List<CommitLogEntry> commitLogEntries) {
        this.commitLogEntries = commitLogEntries;
    }

    public int getLeaderCommitIndex() {
        return leaderCommitIndex;
    }

    public void setLeaderCommitIndex(int leaderCommitIndex) {
        this.leaderCommitIndex = leaderCommitIndex;
    }

    @Override
    public String toString() {
        return "AppendEntriesRequest{" +
                "term=" + term +
                ", leaderAddr='" + leaderAddr + '\'' +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", leaderCommitIndex=" + leaderCommitIndex +
                '}';
    }
}
