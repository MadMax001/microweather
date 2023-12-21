package ru.madmax.pet.microweather.consumer.service;

import ru.madmax.pet.microweather.common.model.MessageDTO;

public interface WeatherListenerService {
    void listen(MessageDTO message,
                String key,
                String topic,
                Integer partition,
                Long offset);
}
