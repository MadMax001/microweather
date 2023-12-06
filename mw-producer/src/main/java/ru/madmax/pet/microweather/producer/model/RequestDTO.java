package ru.madmax.pet.microweather.producer.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.madmax.pet.microweather.common.model.Point;

@Data
public class RequestDTO {                                                                       //todo тесты на валидацию
    @NotNull(message = "Source is not set")
    private String source;
    @Valid
    private Point point;
}
