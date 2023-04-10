package com.tinymq.core.store;

import com.tinymq.core.ConsensusService;
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

    private static final long PAGE_SIZE = 1024 * 4;
    private static final AtomicInteger TOTAL_MAPPED_FILES = new AtomicInteger(0);

    // 写入磁盘的位置
    protected final AtomicInteger writePosition = new AtomicInteger(0);
    protected final AtomicInteger committedPosition = new AtomicInteger(0);


    private String filename;
    private int fileSize;
    private File file;

    protected FileChannel fileChannel;

    protected ByteBuffer writeBuffer;
    protected MappedByteBuffer mappedByteBuffer;

    // 根据文件名获取的偏移长度
    private final long offsetFromFilename;


    public MappedFile(String filename) throws IOException {
        init(filename, (int) PAGE_SIZE);
        this.offsetFromFilename = Long.parseLong(filename);
    }

    private void init(final String filename, final int fileSize) throws IOException {
        this.filename = filename;
        this.fileSize = fileSize;
        this.file = new File(filename);
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

    public void appendMessage(CommitLogEntry commitLogEntry) {
        byte[] bytes = CommitLogEntry.serialize(commitLogEntry);

        // when size less than page_size, use mappedByteBuffer
        try {
            writeBuffer = this.writeBuffer != null ? this.writeBuffer : this.mappedByteBuffer.slice();

            if(bytes.length < PAGE_SIZE) {

            } else {
                this.fileChannel.write(writeBuffer);
            }

            writePosition.getAndAdd(bytes.length);
        } catch (Exception e)  {
            LOG.warn("write error in file {}, write pos {}", bytes.length, writePosition.getAndAdd( - bytes.length));
            writePosition.getAndAdd(- bytes.length);
        }

    }

}
