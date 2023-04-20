package com.tinymq.core.store;

import com.tinymq.core.RegistrationConfig;
import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.exception.AppendLogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

    /**
     * lastApplied <= committed <= logQueue.size()
     */

    private final AtomicInteger commitIndex = new AtomicInteger(-1);

    private final AtomicInteger lastApplied = new AtomicInteger(-1);

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
        List<CommitLogEntry> notApplied = null;

        try {
            final List<LogItem> notAppliedLogical = this.logQueue.subList(begin, end + 1);
            notApplied = new ArrayList<>(notAppliedLogical.size());
            if(!notAppliedLogical.isEmpty()) {
                for (LogItem item : notAppliedLogical) {
                    CommitLogEntry logEntry = this.commitLogService.getByOffset((int) item.getStartOffset());
                    notApplied.add(logEntry);
                }
            }
            return notApplied;
        }catch (Exception e) {
            LOG.error("error occurred, when extract log entry from file", e);
            return null;
        }
    }

    public List<CommitLogEntry> selectNotCommitted() {
        // todo:
        return null;
    }

    public void increCommitIndex() {
        this.commitIndex.incrementAndGet();
    }
    public void setCommitIndex(int commitIndex) {
        this.commitIndex.set(commitIndex);
    }
    /*
    api for outer, will not check
    * */
    public void appendLog(final AppendEntriesRequest appendEntriesRequest) throws AppendLogException {
        // store
        List<CommitLogEntry> entryList = appendEntriesRequest.getCommitLogEntries();
        if(entryList.size() > 0) {
                for (CommitLogEntry logEntry : entryList) {
                    try {
                        // store physically
                        long startPos = this.commitLogService.appendLog(logEntry);
                        // store logically
                        this.logQueue.offer(LogItem.create(appendEntriesRequest.getTerm(), startPos));
                    } catch (AppendLogException e) {
                        LogItem logItem = this.logQueue.pollLast();
                        LOG.error("store err, appendLog store physically exception occurred, poll logical item {}", logItem, e);
                        throw e;
                    }
                }
        }
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
