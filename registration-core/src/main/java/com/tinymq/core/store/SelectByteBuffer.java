package com.tinymq.core.store;

import java.nio.ByteBuffer;

public class SelectByteBuffer {
    private long startOffset;
    private int size;

    private ByteBuffer buffer;

    public SelectByteBuffer() {
    }

    public SelectByteBuffer(long startOffset, int size, ByteBuffer buffer) {
        this.startOffset = startOffset;
        this.size = size;
        this.buffer = buffer;
    }
    public static SelectByteBuffer create(long startOffset, int size, ByteBuffer buffer) {
        return new SelectByteBuffer(startOffset, size, buffer);
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
