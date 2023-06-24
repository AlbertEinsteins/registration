package com.tinymq.core.status;

public interface Codec<T> {
    byte[] encode(final T obj);
    T decode(final byte[] bytes);
}
