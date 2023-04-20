package com.tinymq.core.store;

import cn.hutool.core.lang.Assert;

public class LogItem {
    /* 产生任期 */
    private int term;
    private long startOffset;

    public static LogItem create(int term, long startOffset) {
        LogItem item = new LogItem();
        item.setStartOffset(term);
        item.setStartOffset(startOffset);
        return item;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long startOffset) {
        this.startOffset = startOffset;
    }

    public static boolean equals(LogItem item1, int idx1, LogItem item2, int idx2) {
        // term 和 index 相同，则相同
        item1 = Assert.notNull(item1);
        item2 = Assert.notNull(item2);
        return item1.getTerm() == item2.getTerm()
                && idx1 == idx2;
    }
}
