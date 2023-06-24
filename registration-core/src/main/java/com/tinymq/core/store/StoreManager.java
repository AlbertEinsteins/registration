package com.tinymq.core.store;

import cn.hutool.core.lang.Assert;
import com.tinymq.core.config.RegistrationConfig;
import com.tinymq.core.exception.AppendLogException;
import com.tinymq.core.status.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 提供存储服务
 *      1.实际log的存储
 *      2.存储逻辑上递增的队列，按照term递增
 */
public class StoreManager {
    public static final Logger LOG = LoggerFactory.getLogger(StoreManager.class);

    /* 实际存储数据 */
    private final DefaultCommitLogService commitLogService;
    /* 仅仅存储逻辑位置，即pos和size */
    private final DefaultLogQueue logQueue;

    private final StateMachine stateMachine;


    /**
     * lastApplied <= committed <= logQueue.size()
     */

    private final AtomicInteger commitIndex = new AtomicInteger(-1);

    private final AtomicInteger lastApplied = new AtomicInteger(-1);

    private final RegistrationConfig registrationConfig;

    public StoreManager(RegistrationConfig registrationConfig, final StateMachine stateMachine) {
        this.registrationConfig = registrationConfig;
        this.commitLogService = new DefaultCommitLogService(registrationConfig);
        this.logQueue = new DefaultLogQueue();
        this.stateMachine = stateMachine;

        this.commitLogService.start();
    }

    public void shutdown() {
        this.commitLogService.shutdown();
    }


    /**
     * 获取还没被StateMachine执行的entry
     * @return entries List
     *         or null if exception occurred
     */
    public List<CommitLogEntry> selectNotApplied() {
        final int begin = lastApplied.get();
        final int end = commitIndex.get();

        return selectRange(begin, end + 1);
    }

    // [start, end)
    // ensure return non-null list, but the size of list could be zero.
    private List<CommitLogEntry> selectRange(int start, int end) {
        List<CommitLogEntry> res;

        if(start == -1) {
            return new ArrayList<>();
        }
        end = Math.max(logQueue.size(), end);

        try {
            final List<LogItem> logicalItems = this.logQueue.subList(start, end);
            res = new ArrayList<>(logicalItems.size());
            if(!logicalItems.isEmpty()) {
                for (LogItem item : logicalItems) {
                    CommitLogEntry logEntry = this.commitLogService.getByOffset((int) item.getStartOffset());
                    res.add(logEntry);
                }
            }
            return res;
        }catch (Exception e) {
            LOG.error("error occurred, when extract log entry from file", e);
            return Collections.emptyList();
        }
    }
    /**
     * @param index
     * @return
     */
    public CommitLogEntry getByIndex(int index) {
        if(logQueue.size() == 0 || index == logQueue.size()) {
            return null;
        }

        if(index < 0 || index > logQueue.size()) {
            throw new IndexOutOfBoundsException(String.format("method [getByIndex] get index {%d}", index));
        }
        List<CommitLogEntry> entryList = selectRange(index, index + 1);
        return !entryList.isEmpty() ? entryList.get(0) : null;
    }


    public boolean setCommitIndexAndExec(int saveIndex) {
        if(commitIndex.get() < saveIndex) {
            try {
                for(int i = commitIndex.get() + 1; i <= saveIndex
                        && i < logQueue.size(); i++) {
                    CommitLogEntry entry = getByIndex(i);
                    stateMachine.execute(entry);
                    commitIndex.incrementAndGet();
                }
            } catch (Exception e) {
                LOG.error("Error occurred in execute log to state machine", e);
                return false;
            }
        }
        return true;
    }

    /*
    api for outer, will not check the data
    and immediately return the save index
    * */
    public int appendLog(final List<CommitLogEntry> entryList) throws AppendLogException {
        Assert.notEmpty(entryList);

        // store
        for (CommitLogEntry logEntry : entryList) {
            try {
                // store physically
                long startPos = this.commitLogService.appendLog(logEntry);
                // store logically
                this.logQueue.offer(LogItem.create(logEntry.getCreatedTerm(), startPos));
                return logQueue.size() - 1;
            } catch (AppendLogException e) {
                LogItem logItem = this.logQueue.pollLast();
                LOG.error("store err, appendLog store physically exception occurred, poll logical item {}", logItem, e);
                throw e;
            }
        }
        return -1;        // unreachable ignore this line
    }

    public int appendLog(final CommitLogEntry entryList) throws AppendLogException {
        return appendLog(Collections.singletonList(entryList));
    }

    public boolean isMatch(int prevLogIndex, int prevLogTerm) {
        if(prevLogIndex == -1) {
            return true;
        }
        return prevLogIndex <= this.logQueue.size() - 1
                && this.logQueue.at(prevLogIndex).getTerm() == prevLogTerm;
    }


    public DefaultCommitLogService getCommitLogService() {
        return commitLogService;
    }

    public DefaultLogQueue getLogQueue() {
        return logQueue;
    }

    public RegistrationConfig getRegistrationConfig() {
        return registrationConfig;
    }

    public int getCommittedIndex() {
        return commitIndex.get();
    }

    public int getLastAppliedIndex() {
        return lastApplied.get();
    }


}
