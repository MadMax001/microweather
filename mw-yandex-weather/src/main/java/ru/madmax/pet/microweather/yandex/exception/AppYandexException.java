package ru.madmax.pet.microweather.yandex.exception;

public class AppYandexException extends RuntimeException {
    public AppYandexException(String s) {
        super(s);
    }

    public AppYandexException(Throwable cause) {
        super(cause);
    }
}
