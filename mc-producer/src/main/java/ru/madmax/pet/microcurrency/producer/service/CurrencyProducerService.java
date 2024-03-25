package ru.madmax.pet.microcurrency.producer.service;

import ru.madmax.pet.microcurrency.common.model.MessageDTO;

public interface CurrencyProducerService {
    void produceMessage(String key, MessageDTO message);
}
