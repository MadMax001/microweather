package ru.madmax.pet.microcurrency.consumer.service.converter.model;

import org.springframework.stereotype.Component;
import ru.madmax.pet.microcurrency.consumer.model.ConversionEntity;
import ru.madmax.pet.microcurrency.common.model.Conversion;

@Component
public class ConversionEntityConverter implements ModelConverter<String, Conversion, ConversionEntity> {
    @Override
    public ConversionEntity convert(String key, Conversion model) {
        var entity = new ConversionEntity();
        entity.setId(key);
        entity.setBase(model.getBase());
        entity.setConvert(model.getConvert());
        entity.setBaseAmount(model.getBaseAmount());
        entity.setConversionAmount(model.getConversionAmount());
        return entity;
    }
}
