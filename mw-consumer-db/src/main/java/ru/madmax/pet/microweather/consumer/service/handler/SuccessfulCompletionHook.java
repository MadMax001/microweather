package ru.madmax.pet.microweather.consumer.service.handler;

public class SuccessfulCompletionHook implements OperationHook<String> {
    @Override
    public void accept(String key, String object) {
        throw new UnsupportedOperationException("Unsupported for SuccessfulCompleteHook");
    }

}
