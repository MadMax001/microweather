package ru.madmax.pet.microcurrency.conslogger.service;

import ru.madmax.pet.microcurrency.common.model.MessageDTO;

public interface ConversionListenerService {
    void listen(MessageDTO message,
                String key,
                String topic,
                Integer partition,
                Long offset);
}
