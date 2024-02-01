package ru.madmax.pet.microweather.consumer.service.handler;

public interface Hook<T> {
    void accept (String key, T object);
}
