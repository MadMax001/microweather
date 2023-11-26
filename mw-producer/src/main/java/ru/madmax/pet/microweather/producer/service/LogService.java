package ru.madmax.pet.microweather.producer.service;

public interface LogService {
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(Throwable throwable);
    void error(String message, Throwable throwable);
}
