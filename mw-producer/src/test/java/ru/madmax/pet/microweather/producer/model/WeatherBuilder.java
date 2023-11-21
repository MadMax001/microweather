package ru.madmax.pet.microweather.producer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aWeather")
@With
public class WeatherBuilder implements TestBuilder<Weather>{
    private long now = 1234567890;
    private String infoURL;
    private Double factTemp;
    private Double factWindSpeed;

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
