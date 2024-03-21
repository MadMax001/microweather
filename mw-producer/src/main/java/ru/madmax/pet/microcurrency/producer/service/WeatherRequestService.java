package ru.madmax.pet.microcurrency.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.common.model.Point;
import ru.madmax.pet.microweather.common.model.Weather;
import ru.madmax.pet.microcurrency.producer.model.RequestParams;


public interface WeatherRequestService {
    Mono<Weather> sendRequest(Point point, RequestParams params);
}
