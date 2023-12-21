package ru.madmax.pet.microweather.consumer.service.handler;

import ru.madmax.pet.microweather.common.model.MessageDTO;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SuccessConsumeHandler implements BiConsumer<String, MessageDTO> {
    @Override
    public void accept(String key, MessageDTO messageDTO) {

    }
}
