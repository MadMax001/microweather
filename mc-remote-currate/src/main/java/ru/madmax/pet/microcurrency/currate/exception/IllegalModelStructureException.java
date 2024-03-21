package ru.madmax.pet.microcurrency.currate.exception;

public class IllegalModelStructureException extends RuntimeException{
    private final String modelStringPresentation;

    public IllegalModelStructureException(String message, String modelStringPresentation) {
        super(message);
        this.modelStringPresentation = modelStringPresentation;
    }

    @Override
    public String getMessage() {
        return String.format("%s: %s %s",
                this.getClass().getCanonicalName(),
                super.getMessage(),
                modelStringPresentation);
    }
}
