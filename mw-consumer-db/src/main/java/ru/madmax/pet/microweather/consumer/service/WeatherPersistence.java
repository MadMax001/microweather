package ru.madmax.pet.microweather.consumer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;

public interface WeatherPersistence {
    Mono<WeatherDomain> save(WeatherDomain weather);
}
