package com.tinymq.core.store;

import java.nio.ByteBuffer;

public class CommitLogEntry {
    private int totalSize;
    private int magicCode;

    private int crcCode;
    /* 创建任期 */
    private int createdTerm;

    private int bodyLength;
    private byte[] body;

    /* 是否提交，提交的entry保证一定会被执行 */
    private boolean isCommitted;



    public static CommitLogEntry deserialize(final byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        CommitLogEntry logEntry = new CommitLogEntry();
        logEntry.totalSize = buffer.getInt();
        logEntry.magicCode = buffer.getInt();
        logEntry.createdTerm = buffer.getInt();
        logEntry.bodyLength = buffer.getInt();
        logEntry.body = new byte[logEntry.bodyLength];
        buffer.get(logEntry.body);
        logEntry.isCommitted = buffer.get() == 1;
        return logEntry;
    }


    public static byte[] serialize(final CommitLogEntry logEntry) {
        ByteBuffer buffer = ByteBuffer.allocate(logEntry.getSize());
        buffer.putInt(logEntry.totalSize);
        buffer.putInt(logEntry.magicCode);
        buffer.putInt(logEntry.createdTerm);
        buffer.putInt(logEntry.bodyLength);
        buffer.put(logEntry.body);
        buffer.put(logEntry.isCommitted ? (byte) 1 : (byte) 0);
        return buffer.array();
    }

    private int getSize() {
        // Size of int fields
        //          (totalSize, magicCode, crcCode, createdTerm, bodyLength)
        //          + size of body array + size of boolean field (isCommitted)
        return 5 * Integer.BYTES + bodyLength + Byte.BYTES;
    }
}
