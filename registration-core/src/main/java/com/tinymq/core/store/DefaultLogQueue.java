package com.tinymq.core.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultLogQueue implements LogQueue<LogItem> {
    public static final Logger LOG = LoggerFactory.getLogger(DefaultLogQueue.class);

    private final CopyOnWriteArrayList<LogItem> logItems = new CopyOnWriteArrayList<>();


    @Override
    public boolean offer(LogItem logItem) {

        return this.logItems.add(logItem);
    }

    public LogItem pollLast() {
        if(logItems.isEmpty()) {
            return null;
        }
        return logItems.remove(logItems.size() - 1);
    }

    /* [start, end) */
    public List<LogItem> subList(int start, int end) {
        return logItems.subList(start, end);
    }

    @Override
    public boolean recoverFrom(LogItem[] items) {
        //TODO
        return false;
    }

    @Override
    public boolean recoverFrom(List<LogItem> items) {
        //TODO
        return false;
    }

    @Override
    public int size() {
        return this.logItems.size();
    }

    @Override
    public LogItem at(int idx) {
        if(idx < 0 || idx >= size()) {
            throw new IndexOutOfBoundsException(String.format("logitem at [%d] error", idx));
        }
        return this.logItems.get(idx);
    }
}
