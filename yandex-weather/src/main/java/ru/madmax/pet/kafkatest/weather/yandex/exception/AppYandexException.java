package ru.madmax.pet.kafkatest.weather.yandex.exception;

public class AppYandexException extends RuntimeException {
    public AppYandexException(String s) {
    }

    public AppYandexException(Throwable cause) {
        super(cause);
    }
}
