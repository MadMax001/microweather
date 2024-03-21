package ru.madmax.pet.microcurrency.producer.service;

public interface LogService {
    void info(String key, String message);
    void warn(String message);
    void error(String key, String message);
    void error(Throwable throwable);
    void error(String message, Throwable throwable);
}
