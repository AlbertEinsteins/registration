package com.tinymq.core.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RandomResettableTimer {
    private static final Logger LOG = LoggerFactory.getLogger(RandomResettableTimer.class);

    private final ScheduledThreadPoolExecutor timer;

    private final Runnable timerTask;

    private volatile ScheduledFuture<?> scheduledFuture;
    private final AtomicBoolean startOne = new AtomicBoolean(true);

    private final long delayMillis;

    private final long periodStartMillis;
    private final long periodEndMillis;


    public RandomResettableTimer(final Runnable task,
                                 final long delayMillis, final long periodStartMillis, final long periodEndMillis) {
        this.delayMillis = delayMillis;
        this.periodStartMillis = periodStartMillis;
        this.periodEndMillis = periodEndMillis;
        this.timerTask = task;
        this.timer = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "[VoteScheduleTimer]");
            }
        });
        this.timer.setRemoveOnCancelPolicy(true);
    }

    public void start() {
        if(startOne.compareAndSet(true, false)) {
            scheduledFuture = submitAndGetTimerTask();
        }
    }
    public void shutdown() {
        this.timer.shutdown();
    }

    public void resetTimer() {
        if(Objects.isNull(scheduledFuture)) {
            LOG.info("please start first...");
        } else {
            scheduledFuture.cancel(true);
            timer.getQueue().clear();
            scheduledFuture = submitAndGetTimerTask();
        }
    }

    /**
     * 撤销任务，定时器仍然存在
     */
    public void clearTimer() {
        if(scheduledFuture != null && !scheduledFuture.isDone()) {
            LOG.info("clear election timer...............");
            scheduledFuture.cancel(true);
            timer.getQueue().clear();
        }
    }

    private ScheduledFuture<?> submitAndGetTimerTask() {
        return this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                RandomResettableTimer.this.timerTask.run();
            }
        }, delayMillis,
                ThreadLocalRandom.current().nextLong(periodStartMillis, periodEndMillis),
                TimeUnit.MILLISECONDS);
    }

}
