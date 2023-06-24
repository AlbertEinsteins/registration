package com.tinymq.core.store;

import com.tinymq.core.utils.UtilsAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

public class MappedFileGroup {
    private static final Logger LOG = LoggerFactory.getLogger(MappedFileGroup.class);

    private final CopyOnWriteArrayList<MappedFile> mappedFiles = new CopyOnWriteArrayList<>();
    private final CreateFileService createFileService;
    private final int mappedFileSize;

    public MappedFileGroup(final int mappedFileSize,
                           final CreateFileService createFileService) {
        this.mappedFileSize = mappedFileSize;
        this.createFileService = createFileService;
    }



    // 根据某条日志的偏移量获取对应文件
    // 不存在，则选择创建
    public MappedFile getMappedFile(final int startOffset, boolean isCreate) {
        MappedFile mappedFile = getMappedFile(startOffset);
        if(mappedFile == null) {
            if(!isCreate) {
                return mappedFile;
            }
            //文件起始地址
            int fileStartOffset = startOffset - (startOffset % mappedFileSize);
            mappedFile = this.createFileService.putAndGetMappedFile(UtilsAll.getFileNameFromOffset(fileStartOffset),
                    mappedFileSize);
            return mappedFile;
        }
        return mappedFile;
    }
    public void add(MappedFile mappedFile) {
        if(mappedFile != null) {
            this.mappedFiles.add(mappedFile);
        }
    }

    public MappedFile getMappedFile(final int startOffset) {
        int idx = startOffset / this.mappedFileSize;
        if(idx >= this.mappedFiles.size()) {
            return null;
        }
        return this.mappedFiles.get(idx);
    }

    public MappedFile getLastMappedFile() {
        MappedFile mappedFile = null;
        while(!this.mappedFiles.isEmpty()) {
            try {
                // 第一次创建可能需要点时间
                mappedFile = this.mappedFiles.get(this.mappedFiles.size() - 1);
                break;
            } catch (IndexOutOfBoundsException ignore) {
            } catch (Exception e) {
                LOG.warn("getLastMappedFile exception");
            }

        }
        return mappedFile;
    }


}
