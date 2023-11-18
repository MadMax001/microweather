package ru.madmax.pet.kafkatest.weather.yandex.model;

import lombok.*;

@Data
public class Fact {
    private Double temp;
    private Double windSpeed;
}
