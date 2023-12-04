package ru.madmax.pet.microweather.producer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aRequestDTO")
@With
public class TestRequestDTOBuilder implements TestBuilder<RequestDTO>{
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
