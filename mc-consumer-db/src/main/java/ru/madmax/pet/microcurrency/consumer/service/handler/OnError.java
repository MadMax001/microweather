package ru.madmax.pet.microcurrency.consumer.service.handler;

public class OnError implements Hook<Throwable> {
    @Override
    public void accept(String key, Throwable object) {}
}
