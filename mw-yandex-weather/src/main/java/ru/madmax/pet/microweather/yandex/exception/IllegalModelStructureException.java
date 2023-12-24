package ru.madmax.pet.microweather.yandex.exception;

public class IllegalModelStructureException extends RuntimeException{
    private final String modelStringPresentation;

    public IllegalModelStructureException(String message, String modelStringPresentation) {
        super(message);
        this.modelStringPresentation = modelStringPresentation;
    }

    @Override
    public String getMessage() {
        return String.format("%s: %s",
                this.getClass().getCanonicalName(),
                modelStringPresentation);
    }
}
