package ru.madmax.pet.microcurrency.producer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microweather.common.model.Point;
import ru.madmax.pet.microweather.common.model.TestBuilder;
import ru.madmax.pet.microweather.common.model.TestPointBuilder;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aRequestDTO")
@With
public class TestRequestDTOBuilder implements TestBuilder<RequestDTO> {
    private Point point = TestPointBuilder.aPoint().build();
    private String source = "first";

    @Override
    public RequestDTO build() {
        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setSource(source);
        requestDTO.setPoint(point);
        return requestDTO;
    }
}
