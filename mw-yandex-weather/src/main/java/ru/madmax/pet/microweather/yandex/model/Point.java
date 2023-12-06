package ru.madmax.pet.microweather.yandex.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Builder
@Getter
@Setter
public class Point {
    @NotNull(message = "Latitude is not set")
    private final Double lat;
    @NotNull(message = "Longitude is not set")
    private final Double lon;
}
