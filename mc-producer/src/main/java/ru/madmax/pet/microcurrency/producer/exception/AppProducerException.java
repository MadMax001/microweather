package ru.madmax.pet.microcurrency.producer.exception;


public class AppProducerException extends RuntimeException {
    public AppProducerException(Throwable e) {
        super(e);
    }

    public AppProducerException(String message) {
        super(message);
    }
}
