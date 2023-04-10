package com.tinymq.core;


public class RegistrationConfig {
    private int listenPort;
    private int heartBeatTimeMillis = 1000;

    public static final String DEFAULT_COMMIT_LOG_FILEPATH = System.getProperty("user.home",
            System.getProperty("user.dir"));

    public String commitLogPath = DEFAULT_COMMIT_LOG_FILEPATH;

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

    public String getCommitLogPath() {
        return commitLogPath;
    }

    public RegistrationConfig setCommitLogPath(String commitLogPath) {
        this.commitLogPath = commitLogPath;
        return this;
    }
}
