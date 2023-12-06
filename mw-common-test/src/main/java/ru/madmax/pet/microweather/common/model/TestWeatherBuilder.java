package ru.madmax.pet.microweather.common.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aWeather")
@With
public class TestWeatherBuilder implements TestBuilder<Weather>{
    private long now = 1234567890;
    private String infoURL = "www.test.ru";
    private Double factTemp = 10.0;
    private Double factWindSpeed = 5.3;

    @Override
    public Weather build() {
        Info info = new Info();
        info.setUrl(infoURL);
        Fact fact = new Fact();
        fact.setTemp(factTemp);
        fact.setWindSpeed(factWindSpeed);
        Weather weather = new Weather();
        weather.setNow(now);
        weather.setInfo(info);
        weather.setFact(fact);
        return weather;
    }
}
