package ru.madmax.pet.microweather.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.producer.model.Weather;

public interface WeatherProducerService {
    void produceWeather(Mono<Weather> monoWeather);
}
