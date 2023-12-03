package ru.madmax.pet.microweather.producer.service;

import ru.madmax.pet.microweather.producer.model.Point;
import ru.madmax.pet.microweather.producer.model.RequestParams;

public interface WeatherService {
    void requestAndProduce(Point point, RequestParams params);
}
