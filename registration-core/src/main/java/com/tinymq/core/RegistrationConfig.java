package com.tinymq.core;


import java.io.File;

public class RegistrationConfig {

    //========= status ===============
    private int listenPort;
    private int heartBeatTimeMillis = 1000;

    //============== store ============
    public static final String DEFAULT_COMMIT_LOG_HOME = System.getProperty("com.tinymq.registry.store.home",
            System.getProperty("user.home")) ;
    public static final String DEFUALT_COMMIT_LOG_FILESIZE = System.getProperty("com.tinymq.registry.filesize", "61440");

    private double createNewFileFactor = 0.8;
    private int fileSize = Integer.parseInt(DEFUALT_COMMIT_LOG_FILESIZE);
    private String savePath = DEFAULT_COMMIT_LOG_HOME + File.separator + "store";

    public int getListenPort() {
        return listenPort;
    }

    public RegistrationConfig setListenPort(int listenPort) {
        this.listenPort = listenPort;
        return this;
    }

    public int getHeartBeatTimeMillis() {
        return heartBeatTimeMillis;
    }

    public RegistrationConfig setHeartBeatTimeMillis(int heartBeatTimeMillis) {
        this.heartBeatTimeMillis = heartBeatTimeMillis;
        return this;
    }


    public double getCreateNewFileFactor() {
        return createNewFileFactor;
    }

    public void setCreateNewFileFactor(double createNewFileFactor) {
        this.createNewFileFactor = createNewFileFactor;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }
}
