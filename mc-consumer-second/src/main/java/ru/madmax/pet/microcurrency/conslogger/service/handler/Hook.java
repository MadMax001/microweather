package ru.madmax.pet.microcurrency.conslogger.service.handler;

public interface Hook<T> {
    void accept (String key, T object);
}
