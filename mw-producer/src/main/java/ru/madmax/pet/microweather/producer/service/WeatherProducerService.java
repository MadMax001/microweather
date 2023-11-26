package ru.madmax.pet.microweather.producer.service;

import ru.madmax.pet.microweather.producer.model.Weather;

public interface WeatherProducerService {
    void produceWeather(String key, Weather weather);
}
