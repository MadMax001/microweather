package ru.madmax.pet.microweather.consumer.service.handler;

import org.springframework.stereotype.Component;
import ru.madmax.pet.microweather.common.model.MessageDTO;

@Component
public class ConsumerHook implements OperationHook<MessageDTO> {
    @Override
    public void accept(String key, MessageDTO messageDTO) {}

}
