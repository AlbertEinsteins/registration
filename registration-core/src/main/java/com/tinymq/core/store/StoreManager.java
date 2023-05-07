package com.tinymq.core.store;

import cn.hutool.core.lang.Assert;
import com.tinymq.core.RegistrationConfig;
import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.exception.AppendLogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    /**
     * lastApplied <= committed <= logQueue.size()
     */

    private final AtomicInteger commitIndex = new AtomicInteger(-1);

    private final AtomicInteger lastApplied = new AtomicInteger(0);

    private final RegistrationConfig registrationConfig;

    public StoreManager(RegistrationConfig registrationConfig) {
        this.registrationConfig = registrationConfig;
        this.commitLogService = new DefaultCommitLogService(registrationConfig);
        this.logQueue = new DefaultLogQueue();

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

    public CommitLogEntry getByIndex(int index) {
        if(index < 0 || index >= logQueue.size()) {
            throw new IndexOutOfBoundsException(String.format("method [getByIndex] get index {%d}", index));
        }
        List<CommitLogEntry> entryList = selectRange(index, index + 1);
        return !entryList.isEmpty() ? entryList.get(0) : null;
    }


    public void increCommitIndex() {
        this.commitIndex.incrementAndGet();
    }
    public void setCommitIndex(int commitIndex) {
        this.commitIndex.set(commitIndex);
    }
    /*
    api for outer, will not check
    will return the save index
    * */
    public int appendLog(final List<CommitLogEntry> entryList) throws AppendLogException {
        Assert.notEmpty(entryList);

        // store
        if(!entryList.isEmpty()) {
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
        }
        return -1;        // unreachable ignore this line
    }

    public int appendLog(final CommitLogEntry entryList) throws AppendLogException {
        return appendLog(Collections.singletonList(entryList));
    }

    // todo:
    public int findMatchIndex(int prevLogIndex, int prevLogTerm) {
        int size = logQueue.size();
        if(prevLogIndex >= 0 && prevLogIndex < size) {
            if(logQueue.at(prevLogIndex).getTerm() == prevLogTerm) {
                return prevLogIndex;
            }
        }
        return -1;
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
