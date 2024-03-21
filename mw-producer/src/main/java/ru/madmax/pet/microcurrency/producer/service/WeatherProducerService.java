package ru.madmax.pet.microcurrency.producer.service;

import ru.madmax.pet.microweather.common.model.MessageDTO;

public interface WeatherProducerService {
    void produceMessage(String key, MessageDTO message);
}
