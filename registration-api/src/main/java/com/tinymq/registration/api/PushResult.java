package com.tinymq.registration.api;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PushResult {
    private String info;
    private boolean success;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public PushResult waitResponse(long timeoutMillis) throws InterruptedException {
        countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this;
    }

    public void putResult(String info, boolean isSuccess) {
        this.info = info;
        this.success = isSuccess;
        this.countDownLatch.countDown();
    }


    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
