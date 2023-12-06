package ru.madmax.pet.microweather.yandex.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.yandex.model.Point;
import ru.madmax.pet.microweather.yandex.model.Weather;

public interface WeatherLoaderService {
    Mono<Weather> requestWeatherByPoint(Point point);
}
