package ru.madmax.pet.microweather.weather.yandex.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.weather.yandex.model.Point;
import ru.madmax.pet.microweather.weather.yandex.model.Weather;

public interface WeatherLoaderService {
    Mono<Weather> requestWeatherByPoint(Point point);
}
