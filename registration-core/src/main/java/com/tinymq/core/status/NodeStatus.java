package com.tinymq.core.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class NodeStatus {
    private static final Logger LOG = LoggerFactory.getLogger(NodeStatus.class);
    /* 任期 */
    private AtomicInteger curTerm = new AtomicInteger(0);
    /* 节点状态 */
    private volatile int status;
    /*leaderAddr*/
    private volatile String leaderAddr = "";

    /*投票权*/
    private volatile boolean isVoteAvailable;

    private final ReentrantLock lockPeerNodeStatus = new ReentrantLock();
    private final ConcurrentHashMap</*addr*/String, InnerPeerNodeStatus> peerNodeStatus = new ConcurrentHashMap<>();

    public void setVoteRight(boolean isVoteAvailable) {
        synchronized (this) {
            this.isVoteAvailable = isVoteAvailable;
        }
    }
    public boolean isVoteAvailable() {
        return this.isVoteAvailable;
    }
    public void resetVoteRight() {
        synchronized (this) {
            this.isVoteAvailable = true;
        }
    }

    /*
    * nextIndex, the last log index + 1
    * matchIndex, initialized to 0
    * */
    public void initPeerNodeStatus(final Set<String> nodes, int leaderIndex) {
        if(!nodes.isEmpty()) {
            lockPeerNodeStatus.lock();
            try {
                for (String node : nodes) {
                    peerNodeStatus.putIfAbsent(node, new InnerPeerNodeStatus(leaderIndex + 1, -1));
                }
            } finally {
                lockPeerNodeStatus.unlock();
            }
        }
    }

    public void updatePeerNodeStatus(String nodeAddr, int newNextIndex, int newMatchIndex) {
        InnerPeerNodeStatus nodeStatus = this.peerNodeStatus.get(nodeAddr);
        if(nodeStatus.nextIndex == newNextIndex && nodeStatus.matchIndex == newMatchIndex) {
            return ;
        }
        try {
            lockPeerNodeStatus.lock();
            nodeStatus = this.peerNodeStatus.get(nodeAddr);
            nodeStatus.nextIndex = newNextIndex;
            nodeStatus.matchIndex = newMatchIndex;
        } catch (Exception e) {
            LOG.error("update node {} nextIndex {}->{}, matchIndex {}->{} err occurred", nodeAddr, nodeStatus.nextIndex, newNextIndex,
                    nodeStatus.matchIndex, newMatchIndex, e);
        } finally {
            lockPeerNodeStatus.unlock();
        }
    }

    public InnerPeerNodeStatus getNodeStatus(String nodeAddr) {
        return this.peerNodeStatus.get(nodeAddr);
    }

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

    public void setVoteAvailable(boolean voteAvailable) {
        isVoteAvailable = voteAvailable;
    }

    public void decrementNextIndex(String remoteAddr) {
        try {
            lockPeerNodeStatus.lock();
            InnerPeerNodeStatus sta = peerNodeStatus.get(remoteAddr);
            sta.nextIndex --;
            peerNodeStatus.put(remoteAddr, sta);
        } finally {
            lockPeerNodeStatus.unlock();
        }
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
    static class InnerPeerNodeStatus {
        int nextIndex;
        int matchIndex;

        public InnerPeerNodeStatus(int nextIndex, int matchIndex) {
            this.nextIndex = nextIndex;
            this.matchIndex = matchIndex;
        }

        @Override
        public String toString() {
            return "InnerPeerNodeStatus{" +
                    "nextIndex=" + nextIndex +
                    ", matchIndex=" + matchIndex +
                    '}';
        }
    }
}
