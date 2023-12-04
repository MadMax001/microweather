package ru.madmax.pet.microweather.producer.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class Point {
    @NotNull(message = "Latitude is not set")
    private final Double lat;
    @NotNull(message = "Longitude is not set")
    private final Double lon;
}
