package com.tinymq;

import com.tinymq.core.status.RandomResettableTimer;
import com.tinymq.core.utils.UtilsAll;
import org.junit.Test;

import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.*;

/**
 * Unit test for simple App.
 */
public class AppTest
{
    @Test
    public void getUserHome() {
//        Map<String, String> envMap = System.getenv();
//        for(String key: envMap.keySet()) {
//            System.out.printf("[%s]->[%s]\n", key, envMap.get(key));
//        }
//        System.out.println();
        Properties properties = System.getProperties();
        for(Object k: properties.keySet()) {
            System.out.printf("[%s]->[%s]\n", k, properties.get(k));
        }
        System.out.println(properties.get("user.home"));
    }

    @Test
    public void testTask() throws InterruptedException {
        final ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

        ScheduledFuture<?> scheduledFuture = pool.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("123213");
            }
        }, 300, 2000, TimeUnit.MILLISECONDS);

        scheduledFuture.cancel(false);


        TimeUnit.SECONDS.sleep(100);
    }

    ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    private void resetTimer(ScheduledFuture<?> scheduledFuture) {
        scheduledFuture.cancel(false);
        scheduledFuture = timer.scheduleAtFixedRate(() -> {
            System.out.println("123213");
        }, 1000, 3000, TimeUnit.MILLISECONDS);
        System.out.println(scheduledFuture + "123123");
    }

    @Test
    public void testTimer() throws InterruptedException {

        ScheduledFuture<?> scheduledFuture = timer.scheduleAtFixedRate(() -> {
            System.out.println("123213");
        }, 1000, 3000, TimeUnit.MILLISECONDS);

        TimeUnit.MILLISECONDS.sleep(500);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                resetTimer(scheduledFuture);
                System.out.println(scheduledFuture);
            }
        }, 100, 1000, TimeUnit.MILLISECONDS);

        TimeUnit.SECONDS.sleep(100);
    }

    @Test
    public void testResetTimer() throws InterruptedException {

        RandomResettableTimer resettableTimer = new RandomResettableTimer(() -> {
            System.out.println("123123");
        }, 2000, 3000, 5000);

        resettableTimer.start();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("qingchu");
                resettableTimer.resetTimer();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);

        TimeUnit.SECONDS.sleep(100);
    }


    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        new Thread(() -> {
            latch.countDown();
            latch.countDown();
        }).start();


        latch.await();
        System.out.println("123213");
    }

    @Test
    public void t() {
        System.out.println(UtilsAll.getFileNameFromOffset(1));
    }
}
