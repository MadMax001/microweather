package ru.madmax.pet.microweather.consumer.service.converter.model;

import org.springframework.stereotype.Component;
import ru.madmax.pet.microweather.common.model.Weather;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;

@Component
public class WeatherDomainConverter implements ModelDomainConverter<String, Weather, WeatherDomain> {
    @Override
    public WeatherDomain convert(String key, Weather model) {
        var wd = new WeatherDomain();
        wd.setId(key);
        wd.setTemperature(model.getFact().getTemp());
        wd.setWind(model.getFact().getWindSpeed());
        wd.setNow(model.getNow());
        wd.setUrl(model.getInfo().getUrl());
        return wd;
    }
}
