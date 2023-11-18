package ru.madmax.pet.kafkatest.weather.yandex.model;

import lombok.*;

@Data
public class Weather {
    private long now;
    private Fact fact;
    private Info info;
}
