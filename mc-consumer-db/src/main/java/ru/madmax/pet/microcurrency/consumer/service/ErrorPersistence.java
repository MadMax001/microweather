package ru.madmax.pet.microcurrency.consumer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.consumer.model.ErrorEntity;

public interface ErrorPersistence {
    Mono<ErrorEntity> saveError(ErrorEntity error);
}
