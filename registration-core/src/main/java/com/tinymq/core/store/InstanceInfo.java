package com.tinymq.core.store;

import java.util.Map;

public class InstanceInfo {
    // 实例信息
    private String ip;
    private int port;

    private long registrationTimeStamp;
    private long lastUpdateTimeStamp;

    private Map<String, String> metaFields;


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getRegistrationTimeStamp() {
        return registrationTimeStamp;
    }

    public void setRegistrationTimeStamp(long registrationTimeStamp) {
        this.registrationTimeStamp = registrationTimeStamp;
    }

    public long getLastUpdateTimeStamp() {
        return lastUpdateTimeStamp;
    }

    public void setLastUpdateTimeStamp(long lastUpdateTimeStamp) {
        this.lastUpdateTimeStamp = lastUpdateTimeStamp;
    }

    public Map<String, String> getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(Map<String, String> metaFields) {
        this.metaFields = metaFields;
    }

    @Override
    public String toString() {
        return "InstanceInfo{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", registrationTimeStamp=" + registrationTimeStamp +
                ", lastUpdateTimeStamp=" + lastUpdateTimeStamp +
                '}';
    }
}
