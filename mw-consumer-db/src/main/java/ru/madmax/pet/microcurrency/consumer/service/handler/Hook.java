package ru.madmax.pet.microcurrency.consumer.service.handler;

public interface Hook<T> {
    void accept (String key, T object);
}
