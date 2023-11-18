package ru.madmax.pet.kafkatest.weather.yandex.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.kafkatest.weather.yandex.model.Point;
import ru.madmax.pet.kafkatest.weather.yandex.model.Weather;

public interface WeatherLoaderService {
    Mono<Weather> requestWeatherByPoint(Point point);
}
