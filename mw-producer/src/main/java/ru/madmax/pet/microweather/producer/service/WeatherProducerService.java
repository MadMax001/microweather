package ru.madmax.pet.microweather.producer.service;

import ru.madmax.pet.microweather.common.model.MessageDTO;

public interface WeatherProducerService {
    void produceMessage(String key, MessageDTO message);
}
