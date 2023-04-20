package com.tinymq.core.store;

import com.tinymq.core.utils.UtilsAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class MappedFile {
    private static final Logger LOG = LoggerFactory.getLogger(MappedFile.class);
    // 当文件不足以写入一个Entry时，写入该标志，表示该文件满
    private static final int END_OF_FILE = 0x1234;


    private static final long PAGE_SIZE = 1024 * 4;
    private static final AtomicInteger TOTAL_MAPPED_FILES = new AtomicInteger(0);

    // 待写入磁盘的位置
    protected final AtomicInteger writePosition = new AtomicInteger(0);
    protected final AtomicInteger warmPosition = new AtomicInteger(0);

    private String filename;
    private int fileSize;
    private File file;

    protected FileChannel fileChannel;

    protected MappedByteBuffer mappedByteBuffer;

    // 根据文件名获取的偏移长度
    private long offsetFromFile;

    private final double createFactor;

    public MappedFile(String filename, final double createFactor) throws IOException {
        this(filename, (int) PAGE_SIZE, createFactor);
    }

    public MappedFile(String filename, final int fileSize, final double createFactor) throws IOException {
        init(filename, fileSize);
        this.createFactor = createFactor;
    }

    private void init(final String filename, final int fileSize) throws IOException {
        this.filename = filename;
        this.fileSize = fileSize;
        this.file = new File(filename);
        this.offsetFromFile = Long.parseLong(UtilsAll.splitFileName(filename));
        boolean success = false;
        ensureDirOK(this.file.getParent());

        try {
            this.fileChannel = new RandomAccessFile(this.file, "rw").getChannel();
            this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
            success = true;
        } catch (FileNotFoundException e) {
            LOG.info("file {} is not found", this.file.getAbsoluteFile());
            throw e;
        } catch (IOException e) {
            LOG.info("mappedByteBuffer map file {} failed", this.file.getAbsoluteFile());
            throw e;
        } finally {
            if(!success && this.fileChannel != null) {
                this.fileChannel.close();
            }
        }
    }

    private void ensureDirOK(String parent) {
        if(parent != null) {
            File dir = new File(parent);
            if(!dir.exists()) {
                boolean res = dir.mkdirs();
                LOG.info("create dir {} {}", parent, res ? "Success" : "Failed");
            }
        }
    }

    /**
     * 写日志，返回写入位置
     * @param commitLogEntry
     * @return
     */
    public long appendLog(CommitLogEntry commitLogEntry) {
        byte[] bytes = CommitLogEntry.serialize(commitLogEntry);
        return this.appendLog(bytes, 0, bytes.length);
    }

    public long appendLog(final byte[] bytes, int offset, int length) {
        final int writePos = writePosition.get();
        if(writePos + bytes.length > fileSize) {
            LOG.warn("the rest space of the file {} is not enough, append false", filename);
            return -1;
        }
        try {
            this.fileChannel.position(writePos);
            this.fileChannel.write(ByteBuffer.wrap(bytes, offset, length));
            writePosition.getAndAdd(bytes.length);
            return writePos;
        } catch (Exception e)  {
            LOG.warn("write {} bytes error in file {}, write pos {}", bytes.length, filename, writePos);
            return -1;
        }
    }

    public boolean syncToMappedBuffer(final byte[] bytes) {
        final int warmPos = warmPosition.get();
        if(warmPos + bytes.length > writePosition.get()) {
            LOG.warn("the sync method can not over the write pos {}, cur warm pos {}, size {}", writePosition.get(),
                    warmPos, bytes.length);
            return false;
        }
        try {
            ByteBuffer slice = mappedByteBuffer.slice();
            slice.position(warmPos);
            slice.limit(warmPos + bytes.length);
            slice.put(bytes);
            warmPosition.getAndAdd(bytes.length);
            return true;
        } catch (Exception e) {
            LOG.warn("syncToMappedBuffer write mappedBytebuffer error, warmPos {}, size {}", warmPos, bytes.length, e);
        }
        return false;
    }

    public SelectByteBuffer selectByteBuffer(int pos) {
        if(pos >= writePosition.get()) {
            LOG.warn("can not read, data in pos {} more than the acceptable index", pos);
            return null;
        }
        int totalSize = this.mappedByteBuffer.getInt(pos);
        return selectByteBuffer(pos, totalSize);
    }

    public SelectByteBuffer selectByteBuffer(int pos, int size) {
        // less that 4K, use MappedByteBuffer
        // otherwise, use FileChannel
        ByteBuffer buffer = null;
        if(pos + size > writePosition.get()) {
            LOG.info("can not read, data in pos {} and size {} more than the acceptable index", pos, size);
            return null;
        }

        try {
            if(size <= PAGE_SIZE) {
                buffer = this.mappedByteBuffer.slice();
                buffer.position(pos);
                buffer.limit(pos + size);
            } else {
                buffer = ByteBuffer.allocate(size);
                fileChannel.position(pos);
                fileChannel.read(buffer);
            }
            return SelectByteBuffer.create(this.offsetFromFile + pos, size, buffer);
        } catch (Exception e) {
            LOG.warn("exception occurred, when read file {} in pos {}", filename, pos);
        }
        return null;
    }

    public void warm() {
        // load data to mappedByteBuffer
        int lastWarmPos = warmPosition.get();
        int writePos = writePosition.get();
        for(int i = lastWarmPos; i < writePos; i += PAGE_SIZE) {
            SelectByteBuffer selBuf = selectByteBuffer(i, (int) (i + Math.min(writePosition.get(), PAGE_SIZE)));
            this.mappedByteBuffer.put(selBuf.getBuffer());
        }
    }

    public void flush() {
        try {
            this.fileChannel.force(false);
        } catch (IOException e) {
            LOG.info("fileChannel force to disk err", e);
        }
    }

    public long getOffsetFromFile() {
        return offsetFromFile;
    }

    public void setOffsetFromFile(long offsetFromFile) {
        this.offsetFromFile = offsetFromFile;
    }

    public boolean isFull() {
        return this.writePosition.get() == this.fileSize || this.writePosition.get() == END_OF_FILE;
    }

    public boolean isExceedThreshold() {
        return this.writePosition.get() >= this.fileSize * this.createFactor;
    }

}
