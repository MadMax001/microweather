package ru.madmax.pet.microweather.producer.model;

import lombok.Data;

@Data
public class Weather {
    private long now;
    private Fact fact;
    private Info info;
}
