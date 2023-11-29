package ru.madmax.pet.microweather.producer.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

@Builder
@Getter
@Setter
public class Point {                                                //todo Обрабатывать ошибки валидации в контроллере
    @NotNull(message = "Latitude is not set")
    private final Double lat;
    @NotNull(message = "Longitude is not set")
    private final Double lon;
}
