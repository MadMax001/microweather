package ru.madmax.pet.microweather.producer.exception;

import java.net.SocketException;

public class AppProducerException extends RuntimeException {
    public AppProducerException(SocketException e) {
        super(e);
    }
}
