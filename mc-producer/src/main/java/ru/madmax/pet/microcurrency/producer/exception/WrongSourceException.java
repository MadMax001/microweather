package ru.madmax.pet.microcurrency.producer.exception;

public class WrongSourceException extends RuntimeException{
    public WrongSourceException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return "Wrong source";
    }
}
