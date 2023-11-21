package ru.madmax.pet.microweather.producer.model;

import lombok.Data;

@Data
public class Fact {
    private Double temp;
    private Double windSpeed;
}
