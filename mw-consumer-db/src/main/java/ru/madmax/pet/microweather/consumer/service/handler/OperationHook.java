package ru.madmax.pet.microweather.consumer.service.handler;

public interface OperationHook<T> {
    void accept (String key, T object);
}
