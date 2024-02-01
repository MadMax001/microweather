package ru.madmax.pet.microweather.consumer.service.handler;

public class OnError implements Hook<Throwable> {
    @Override
    public void accept(String key, Throwable object) {}
}
