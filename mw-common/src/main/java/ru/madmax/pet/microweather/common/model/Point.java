package ru.madmax.pet.microweather.common.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
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
