package ru.madmax.pet.microcurrency.consumer.service.handler;

import org.springframework.stereotype.Component;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;

@Component
public class OnConsume implements Hook<MessageDTO> {
    @Override
    public void accept(String key, MessageDTO messageDTO) {}

}
