package ru.madmax.pet.microweather.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.producer.model.RequestDTO;

public interface WeatherService {
    Mono<String> registerRequest(RequestDTO request);
}
