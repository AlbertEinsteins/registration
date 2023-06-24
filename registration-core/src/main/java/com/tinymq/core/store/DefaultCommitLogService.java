package com.tinymq.core.store;

import com.tinymq.core.config.RegistrationConfig;
import com.tinymq.core.exception.AppendLogException;
import com.tinymq.core.utils.UtilsAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultCommitLogService implements CommitLogService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCommitLogService.class);

    private final MappedFileGroup mappedFileGroup;
    private final CreateFileService createFileService;

    private final AtomicBoolean firstCreate = new AtomicBoolean(false);

    private final RegistrationConfig registrationConfig;


    public DefaultCommitLogService(final RegistrationConfig registrationConfig) {
        this.registrationConfig = registrationConfig;
        this.createFileService = new CreateFileService(registrationConfig.getSavePath(),
                registrationConfig.getCreateNewFileFactor(), this);
        this.mappedFileGroup = new MappedFileGroup(registrationConfig.getFileSize(), createFileService);

    }

    public void start() {
        this.createFileService.start();
    }

    public void shutdown() {
        this.createFileService.shutdown(false);
    }


    @Override
    public long appendLog(final CommitLogEntry commitLogEntry) throws AppendLogException {
        MappedFile lastFile = null;
        if(firstCreate.compareAndSet(false, true)) {
            lastFile = mappedFileGroup.getMappedFile(0, true);
        } else {
            lastFile = mappedFileGroup.getLastMappedFile();
        }

        try {
            long startPos = lastFile.appendLog(commitLogEntry);
            if(startPos >= 0) {
                // sync to mmap
                lastFile.syncToMappedBuffer(CommitLogEntry.serialize(commitLogEntry));

                if(lastFile.isExceedThreshold()) {
                    long newFileStartOffset = lastFile.getOffsetFromFile() + registrationConfig.getFileSize();
                    //async create new file
                    this.createFileService.putRequest(
                            UtilsAll.getFileNameFromOffset(newFileStartOffset),
                            registrationConfig.getFileSize());
                }
                return startPos;
            }
            return -1;
        } catch (Exception e) {
            throw new AppendLogException(lastFile, "appendlog exception", e);
        }
    }

    @Override
    public CommitLogEntry getByOffset(int offset) {
        MappedFile mappedFile = this.mappedFileGroup.getMappedFile(offset);
        if(mappedFile != null) {
            SelectByteBuffer selectByteBuffer = mappedFile.selectByteBuffer(offset);
            return CommitLogEntry.deserialize(
                    selectByteBuffer.getBuffer());
        }
        LOG.warn("read offset {}, but it dose not match any file", offset);
        return null;
    }

    public MappedFileGroup getMappedFileGroup() {
        return mappedFileGroup;
    }
}
