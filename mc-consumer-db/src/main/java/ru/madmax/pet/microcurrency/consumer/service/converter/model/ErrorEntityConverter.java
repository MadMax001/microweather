package ru.madmax.pet.microcurrency.consumer.service.converter.model;

import org.springframework.stereotype.Component;
import ru.madmax.pet.microcurrency.consumer.model.ErrorEntity;

@Component
public class ErrorEntityConverter implements ModelConverter<String, String, ErrorEntity> {
    @Override
    public ErrorEntity convert(String key, String model) {
        var ed = new ErrorEntity();
        ed.setId(key);
        ed.setDetails(model);
        return ed;
    }
}
