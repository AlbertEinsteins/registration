package com.tinymq.core.store;

import com.tinymq.common.utils.ServiceThread;
import com.tinymq.core.utils.UtilsAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/*
asynchronous create file in advance
 */
public class CreateFileService extends ServiceThread {
    private static final Logger LOG = LoggerFactory.getLogger(CreateFileService.class);

    private static final int WAIT_TIMEOUT = 5 * 1000;
    private final double createFactor;
    private final String storePath;
    private final DefaultCommitLogService commitLogService;

    public CreateFileService(final String storePath, double createFactor,
                             final DefaultCommitLogService commitLogService) {
        this.storePath = storePath;
        this.createFactor = createFactor;
        this.commitLogService = commitLogService;
    }

    private final LinkedBlockingQueue<CreateFileRequest> tasks = new LinkedBlockingQueue<>();
    public MappedFile putAndGetMappedFile(String fileName, int fileSize) {
        LOG.info("[putAndGetMappedFile]-push a create file {} request, waiting for creating ", fileName);

        CreateFileRequest createFileRequest = new CreateFileRequest(concatFilename(fileName), fileSize);
        this.tasks.offer(createFileRequest);
        try {
            if(createFileRequest.getLatch().await(WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                LOG.info("[putAndGetMappedFile]-push a create file {} request, creat ok", fileName);
                return createFileRequest.getMappedFile();
            } else {
                LOG.warn("[putAndGetMappedFile]-wait file {} to be created timeout", fileName);
            }
        } catch (Exception e) {
            LOG.error("[putAndGetMappedFile]-push a create file {} request, exception occurred", fileName);
        }

        return null;
    }

    public void putRequest(String fileName, int fileSize) {
        LOG.info("push a create file {} request ", fileName);
        CreateFileRequest createFileRequest = new CreateFileRequest(fileName, fileSize);
        this.tasks.offer(createFileRequest);
    }
    @Override
    public String getServiceName() {
        return CreateFileService.class.getSimpleName();
    }

    @Override
    public void run() {
        while(!isStop) {
            mapFile();
        }
    }

    private void mapFile() {
         CreateFileRequest createFileRequest = null;
        try {
            createFileRequest = this.tasks.take();

            //use default creating method
            MappedFile mappedFile = new MappedFile(createFileRequest.getFilePath(),
                    createFileRequest.getFileSize(), createFactor);
            createFileRequest.setMappedFile(mappedFile);
            commitLogService.getMappedFileGroup().add(mappedFile);
        } catch (InterruptedException e) {
            LOG.info("[mapFile] exception occurred when take a request from tasks");
        } catch (IOException e) {
            LOG.info("[mapFile] create file exception");
        } finally {
            if(createFileRequest != null) {
                createFileRequest.getLatch().countDown();
            }
        }
    }
    private String concatFilename(String filename) {
        return storePath + File.separator + filename;
    }

    static class CreateFileRequest {
        private String filePath;
        private int fileSize;
        private MappedFile mappedFile;
        private final CountDownLatch latch = new CountDownLatch(1);

        public CreateFileRequest(String filePath, int fileSize) {
            this.filePath = filePath;
            this.fileSize = fileSize;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public int getFileSize() {
            return fileSize;
        }

        public void setFileSize(int fileSize) {
            this.fileSize = fileSize;
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        public MappedFile getMappedFile() {
            return mappedFile;
        }

        public void setMappedFile(MappedFile mappedFile) {
            this.mappedFile = mappedFile;
        }
    }
}
