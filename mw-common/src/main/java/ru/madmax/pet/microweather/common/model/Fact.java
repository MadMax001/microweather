package ru.madmax.pet.microweather.common.model;

import lombok.Data;

@Data
@Deprecated(forRemoval = true)
public class Fact {
    private Double temp;
    private Double windSpeed;
}
