package ru.madmax.pet.microweather.common.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aPoint")
@With
public class TestPointBuilder implements TestBuilder<Point>{
    private Double lat = 51.534986;
    private Double lon = 46.001373;

    @Override
    public Point build() {
        return Point.builder()
                .lat(lat)
                .lon(lon)
                .build();
    }
}
