package ru.madmax.pet.microweather.producer.service.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.madmax.pet.microweather.producer.service.LogService;

import java.util.function.BiConsumer;

import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
public class ErrorSendingHandler implements BiConsumer<String, Throwable> {
    private final LogService logService;

    @Override
    public void accept(String key, Throwable error) {
        logService.error(
                key,
                String.format("Error on sending: %s: %s%nCause: %s: %s",
                    error.getClass().getName(),
                    error.getMessage(),
                    (nonNull(error.getCause())? error.getCause().getClass().getName(): ""),
                    (nonNull(error.getCause())? error.getMessage(): "")
        ));    }
}
