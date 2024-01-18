package ru.madmax.pet.microweather.consumer.service.converter.model;

import org.springframework.stereotype.Component;
import ru.madmax.pet.microweather.consumer.model.ErrorDomain;

@Component
public class ErrorDomainConverter implements ModelDomainConverter<String, String, ErrorDomain> {
    @Override
    public ErrorDomain convert(String key, String model) {
        var ed = new ErrorDomain();
        ed.setId(key);
        ed.setDetails(model);
        return ed;
    }
}
