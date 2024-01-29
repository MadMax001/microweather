package ru.madmax.pet.microweather.consumer.service.handler;

public class ErrorCompletionHook implements OperationHook<Throwable>{
    @Override
    public void accept(String key, Throwable object) {}
}
