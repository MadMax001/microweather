package ru.madmax.pet.microcurrency.conslogger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Slf4JLogService implements LogService {
    @Override
    public void info(String key, String message) {
        log.info(String.format("[%s]: %s", key, message));
    }

    @Override
    public void warn(String message) {
        log.warn(message);
    }

    @Override
    public void error(String key, String message) {
        log.error(String.format("[%s]: %s", key, message));
    }

    @Override
    public void error(Throwable throwable) {
        error("Application error", throwable);
    }

    @Override
    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }
}
