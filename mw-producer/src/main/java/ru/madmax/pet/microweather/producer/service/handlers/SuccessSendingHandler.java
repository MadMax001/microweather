package ru.madmax.pet.microweather.producer.service.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.madmax.pet.microweather.producer.model.MessageDTO;
import ru.madmax.pet.microweather.producer.service.LogService;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class SuccessSendingHandler implements BiConsumer<String, MessageDTO> {
    private final LogService logService;

    @Override
    public void accept(String key, MessageDTO value) {
        logService.info(String.format("Successful sending[%s]: %s",
                key,
                value));
    }


}
