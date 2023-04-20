package com.tinymq.core.status;

import java.util.concurrent.atomic.AtomicInteger;

public class NodeStatus {
    /* 任期 */
    private AtomicInteger curTerm = new AtomicInteger(0);
    /* 节点状态 */
    private volatile int status;

    /*leaderAddr*/
    private volatile String leaderAddr = "";

    private volatile int[] nextIndex;
    private volatile int[] matchIndex;

    public int incrementTerm() {
        return curTerm.incrementAndGet();
    }

    public void setStatus(STATUS status) {
        this.status = status.code;
    }

    public STATUS getStatus() {
        return STATUS.fromKey(this.status);
    }

    public AtomicInteger getCurTerm() {
        return curTerm;
    }

    public void setCurTerm(AtomicInteger curTerm) {
        this.curTerm = curTerm;
    }

    public void setLeaderAddr(final String addr) {
        if(leaderAddr.equals(addr)) {
            return ;
        }
        this.leaderAddr = addr;
    }

    public String getLeaderAddr() {
        return leaderAddr;
    }


    public enum STATUS {
        LEADER(0),
        FOLLOWER(1),
        CANDIDATE(2);

        final int code;
        STATUS(int code) {
            this.code = code;
        }

        public static STATUS fromKey(int code) {
            for(STATUS status: STATUS.values()) {
                if(status.code == code) {
                    return status;
                }
            }
            return null;
        }
    }
}
