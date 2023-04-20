package com.tinymq.core.dto.outer;


/**
 * 请求，和响应所需
 *  抽象Command，待写入到StateMachine
 * @param <K>
 * @param <V>
 */
public interface StateModel<K, V> {
    K getKey();

    V getVal(K key);
}
