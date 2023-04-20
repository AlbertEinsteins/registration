package com.tinymq.core.store;


import com.tinymq.core.utils.UtilsAll;

import java.nio.ByteBuffer;

public class CommitLogEntry {
    public static int MAGIC_CODE_V1 = 0x1234;
    private int totalSize;
    private int magicCode;
    private int bodyCrc;
    /* 创建任期 */
    private int createdTerm;
    private int bodyLength;
    private transient byte[] body;

    public static CommitLogEntry createUnCommited(int term, byte[] body) {
        return create(term, body);
    }
    public static CommitLogEntry create(int term, byte[] body) {
        CommitLogEntry commitLogEntry = new CommitLogEntry();
        commitLogEntry.setBodyCrc(UtilsAll.crc32(body));
        commitLogEntry.setMagicCode(MAGIC_CODE_V1);
        commitLogEntry.setCreatedTerm(term);
        commitLogEntry.setBodyLength(body.length);
        commitLogEntry.setBody(body);
        commitLogEntry.setTotalSize(CommitLogEntry.getSize(commitLogEntry));
        return commitLogEntry;
    }


    public static CommitLogEntry deserialize(final byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return deserialize(buffer);
    }
    public static CommitLogEntry deserialize(final ByteBuffer buffer) {
        CommitLogEntry commitLogEntry = new CommitLogEntry();
        commitLogEntry.totalSize = buffer.getInt();
        commitLogEntry.magicCode = buffer.getInt();
        commitLogEntry.createdTerm = buffer.getInt();
        commitLogEntry.bodyLength = buffer.getInt();
        commitLogEntry.body = new byte[commitLogEntry.bodyLength];
        buffer.get(commitLogEntry.body);
        return commitLogEntry;
    }


    public static byte[] serialize(final CommitLogEntry commitLogEntry) {
        ByteBuffer buffer = ByteBuffer.allocate(commitLogEntry.totalSize);
        buffer.putInt(commitLogEntry.totalSize);
        buffer.putInt(commitLogEntry.magicCode);
        buffer.putInt(commitLogEntry.createdTerm);
        buffer.putInt(commitLogEntry.bodyLength);
        buffer.put(commitLogEntry.body);
        return buffer.array();
    }

    public static int getSize(CommitLogEntry commitLogEntry) {
        // Size of int fields
        //          (totalSize, magicCode, bodyCrc, createdTerm, bodyLength)
        //          + size of body array + size of boolean field (isCommitted)
        if(commitLogEntry.getBody() == null) {
            return 5 * Integer.BYTES + 0;
        } else {
            return 5 * Integer.BYTES + commitLogEntry.getBody().length;
        }
    }


    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getMagicCode() {
        return magicCode;
    }

    public void setMagicCode(int magicCode) {
        this.magicCode = magicCode;
    }

    public int getBodyCrc() {
        return bodyCrc;
    }

    public void setBodyCrc(int bodyCrc) {
        this.bodyCrc = bodyCrc;
    }

    public int getCreatedTerm() {
        return createdTerm;
    }

    public void setCreatedTerm(int createdTerm) {
        this.createdTerm = createdTerm;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

}
