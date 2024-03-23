package ru.madmax.pet.microcurrency.consumer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.consumer.model.ConversionEntity;

public interface ConversionPersistence {
    Mono<ConversionEntity> save(ConversionEntity weather);
}
