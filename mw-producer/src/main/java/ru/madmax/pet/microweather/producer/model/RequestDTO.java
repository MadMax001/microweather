package ru.madmax.pet.microweather.producer.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.madmax.pet.microweather.common.model.Point;

@Data
public class RequestDTO {
    @NotNull(message = "Source is not set")
    private String source;
    @Valid
    @NotNull(message = "Point is not set")
    private Point point;
}
