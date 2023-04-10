package com.tinymq.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServiceThread implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceThread.class);

    private static final int JOIN_TIME = 90 * 1000;
    protected final Thread thread;
    protected volatile boolean hasNotified;
    protected volatile boolean isStop;

    public abstract String getServiceName();

    public ServiceThread() {
        this.thread = new Thread(this, getServiceName());
    }


    public void start() {
        this.thread.start();
    }

    public void shutdown() { this.shutdown(false); }

    public void shutdown(boolean interrupt) {
        this.isStop = true;
        LOGGER.info("shutdown thread " + getServiceName() + ", is interrupt: " + interrupt);
        synchronized (this) {
            if(!this.hasNotified) {
                this.hasNotified = true;
                this.notify();
            }

            try {
                if(interrupt) {
                    this.thread.interrupt();
                }

                long beginTimeStamp = System.currentTimeMillis();
                this.thread.join(JOIN_TIME);
                long endTimeStamp = System.currentTimeMillis();
                LOGGER.info("service thread " + getServiceName() + " join time " + (endTimeStamp - beginTimeStamp));
            } catch (InterruptedException e) {
                LOGGER.warn("service thread " + getServiceName() + " interrupt exception occurred");
            }
        }
    }
}
