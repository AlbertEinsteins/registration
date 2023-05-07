package com.tinymq;

import org.junit.Test;

import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class CyclicBarrierTest {
    private final CyclicBarrier cyclicBarrier = new CyclicBarrier(3, new Runnable() {
        @Override
        public void run() {
            System.out.println("------------------------three threads finished----------------");
        }
    });

    class T extends Thread {

        public T(String name) {
            super(name);
        }

        @Override
        public void run() {
            while(true) {

                System.out.println(Thread.currentThread().getName() + ": doing, then waiting");
                try {
                    TimeUnit.SECONDS.sleep(1);
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Test
    public void test3Threads() throws InterruptedException {
        Thread t1 = new T("123");
        Thread t2 = new T("455");
        Thread t3 = new T("111");
        final int repeatTimes = 10;

        t1.start();
        t2.start();
        t3.start();


        TimeUnit.SECONDS.sleep(100);
    }
}
