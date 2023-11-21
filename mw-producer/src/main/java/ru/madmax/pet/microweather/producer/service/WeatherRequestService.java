package ru.madmax.pet.microweather.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.producer.model.Point;
import ru.madmax.pet.microweather.producer.model.RequestParams;
import ru.madmax.pet.microweather.producer.model.Weather;


public interface WeatherRequestService {
    Mono<Weather> registerRequest(Point point, RequestParams params);
}
