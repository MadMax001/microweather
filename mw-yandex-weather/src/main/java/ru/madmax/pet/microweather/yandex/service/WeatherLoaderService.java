package ru.madmax.pet.microweather.yandex.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.common.model.Point;
import ru.madmax.pet.microweather.common.model.Weather;

public interface WeatherLoaderService {
    Mono<Weather> requestWeatherByPoint(Point point);
}
