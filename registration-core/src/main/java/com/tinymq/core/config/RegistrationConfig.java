package com.tinymq.core.config;

import java.io.File;
import java.util.*;

public class RegistrationConfig {
    public static final String DEFAULT_REGISTRATION_FILE_PATH = "registration.yaml";

    private static final String[] necessaryProperties = new String[]{
            "addrNodes",
            "listenPort"
    };

    public RegistrationConfig(String configFilePath) {
        ResolveFileConfig resolveFileConfig = new ResolveFileConfig(configFilePath);
        ensureNecessaryProperty(resolveFileConfig);

        this.addrNodes = new HashSet<>((List) resolveFileConfig.fromKey("addrNodes"));
        this.listenPort = (int)resolveFileConfig.fromKey("listenPort");
        this.selfAddr = (String)resolveFileConfig.fromKey("selfAddr");
    }

    private void ensureNecessaryProperty(ResolveFileConfig resolveFileConfig) {
        for (String name: necessaryProperties) {
            if (resolveFileConfig.fromKey(name) == null) {
                throw new RuntimeException(String.format("The property {%s} in config file must not be null", name));
            }
        }
    }


    //========= status ===============
    private final String selfAddr;

    private int listenPort = 7800;
    private int heartBeatTimeMillis = 1000;
    private int publicThreadPoolSize = 8;

    private Set<String> addrNodes;

    //============== store ============
    public static final String DEFAULT_COMMIT_LOG_HOME = System.getProperty("com.tinymq.registry.store.home",
            System.getProperty("user.home"));
    public static final String DEFUALT_COMMIT_LOG_FILESIZE = System.getProperty("com.tinymq.registry.filesize", "61440");

    private double createNewFileFactor = 0.8;
    private int fileSize = Integer.parseInt(DEFUALT_COMMIT_LOG_FILESIZE);
    private String savePath = DEFAULT_COMMIT_LOG_HOME + File.separator + "store";


    //============== getter setter =====================
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

    public int getPublicThreadPoolSize() {
        return publicThreadPoolSize;
    }

    public Set<String> getAddrNodes() {
        return addrNodes;
    }

    public void setAddrNodes(Set<String> addrNodes) {
        this.addrNodes = addrNodes;
    }

    public String getSelfAddr() {
        return selfAddr;
    }
}
