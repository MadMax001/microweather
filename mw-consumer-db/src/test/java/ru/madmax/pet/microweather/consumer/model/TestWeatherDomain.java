package ru.madmax.pet.microweather.consumer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microweather.common.model.TestBuilder;
import ru.madmax.pet.microweather.common.model.TestWeatherBuilder;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aWeatherDomain")
@With
public class TestWeatherDomain implements TestBuilder<WeatherDomain> {
    private String id = "test_domain_key";

    @Override
    public WeatherDomain build() {
        WeatherDomain wd = new WeatherDomain();
        wd.setId(id);
        var weather =  TestWeatherBuilder.aWeather().build();
//        wd.setTemperature(weather.getFact().getTemp());
//        wd.setWind(weather.getFact().getWindSpeed());
//        wd.setUrl(weather.getInfo().getUrl());
        wd.setNow(weather.getNow());
        wd.setFact(weather.getFact());
        wd.setInfo(weather.getInfo());
        return wd;
    }
}
