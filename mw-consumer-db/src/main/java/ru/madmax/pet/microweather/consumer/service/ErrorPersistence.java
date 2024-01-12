package ru.madmax.pet.microweather.consumer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.consumer.model.ErrorDomain;

public interface ErrorPersistence {
    Mono<ErrorDomain> saveError(ErrorDomain error);
}
