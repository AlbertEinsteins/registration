package com.tinymq.core.store;

import java.util.List;

/**
 * 逻辑队列
 * @param <V> 存储的逻辑项，不含数数据;
 *             必须有两个数据（term，实际存储的下标 ）
 */
public interface LogQueue<V> {

    boolean offer(V v);

    boolean recoverFrom(final V[] items);

    boolean recoverFrom(List<V> items);

    int size();

    LogItem at(int idx);


}
