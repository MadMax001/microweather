package ru.madmax.pet.microweather.common.model;

import lombok.Data;

@Data
public class Weather {
    private long now;
    private Fact fact;
    private Info info;
}
