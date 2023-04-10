package com.tinymq.remote.netty;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class SemaphoreReleaseOnlyOnce {

    private final Semaphore semaphore;

    private final AtomicBoolean releaseOnce = new AtomicBoolean(true);

    public SemaphoreReleaseOnlyOnce(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public void release() {
        if(semaphore != null) {
            if(this.releaseOnce.compareAndSet(true, false)) {
                this.semaphore.release();
            }
        }
    }
}
