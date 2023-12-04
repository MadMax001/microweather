package ru.madmax.pet.microweather.producer.service;

import ru.madmax.pet.microweather.producer.model.RequestDTO;

public interface WeatherService {
    String registerRequest(RequestDTO request);
}
