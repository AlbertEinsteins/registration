package com.tinymq.common.dto;

public class WatcherDto {
    private String clientAddr;
    private String watcherKey;

    private String oldVal;
    private String newVal;

    public static WatcherDto create(String clientAddr, String watcherKey) {
        return new WatcherDto(clientAddr, watcherKey);
    }

    public WatcherDto() {}
    public WatcherDto(String clientAddr, String watcherKey) {
        this.clientAddr = clientAddr;
        this.watcherKey = watcherKey;
    }

    public static WatcherDto create(String clientAddr, String watcherKey, String oldVal, String newVal) {
        WatcherDto watcherDto = new WatcherDto();
        watcherDto.clientAddr = clientAddr;
        watcherDto.watcherKey = watcherKey;
        watcherDto.oldVal = oldVal;
        watcherDto.newVal = newVal;
        return watcherDto;
    }

    public String getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(String clientAddr) {
        this.clientAddr = clientAddr;
    }

    public String getWatcherKey() {
        return watcherKey;
    }

    public void setWatcherKey(String watcherKey) {
        this.watcherKey = watcherKey;
    }

    public String getOldVal() {
        return oldVal;
    }

    public void setOldVal(String oldVal) {
        this.oldVal = oldVal;
    }

    public String getNewVal() {
        return newVal;
    }

    public void setNewVal(String newVal) {
        this.newVal = newVal;
    }
}
