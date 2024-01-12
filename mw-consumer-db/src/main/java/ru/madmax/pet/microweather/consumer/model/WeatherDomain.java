package ru.madmax.pet.microweather.consumer.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "weather", schema = "public")
public class WeatherDomain /*extends Weather*/ {

    private String id;
    private long now;
    private Double temperature;
    private Double wind;
    private String url;

}
