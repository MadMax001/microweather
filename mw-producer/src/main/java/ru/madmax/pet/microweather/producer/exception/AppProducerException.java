package ru.madmax.pet.microweather.producer.exception;


public class AppProducerException extends RuntimeException {
    public AppProducerException(Exception e) {
        super(e);
    }

    public AppProducerException(String message) {
        super(message);
    }
}
