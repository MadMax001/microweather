package ru.madmax.pet.microweather.consumer.configuration;

import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class ConsumerBarrierReady {
    private final CountDownLatch latch;

    public ConsumerBarrierReady() {
        latch = new CountDownLatch(1);
    }

    public void countDown() {
        latch.countDown();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }



}
