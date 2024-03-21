package ru.madmax.pet.microcurrency.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.producer.model.RequestDTO;

public interface WeatherService {
    Mono<String> registerRequest(RequestDTO request);
}
