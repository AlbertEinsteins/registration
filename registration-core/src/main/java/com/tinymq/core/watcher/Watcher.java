package com.tinymq.core.watcher;

import java.util.Objects;

public class Watcher {
    private String clientAddr;


    public Watcher() {
    }

    public Watcher(String clientAddr) {
        this.clientAddr = clientAddr;
    }

    public String getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(String clientAddr) {
        this.clientAddr = clientAddr;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientAddr);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Watcher)) {
            return false;
        }
        Watcher other = (Watcher) obj;
        return this.getClientAddr().equals(other.getClientAddr());
    }

}
