package ru.madmax.pet.microcurrency.currate.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.madmax.pet.microcurrency.currate.misc.ConversionDeserializer;
import ru.madmax.pet.microweather.common.model.ConversionResponse;
import ru.madmax.pet.microweather.common.model.Currency;

import java.math.BigDecimal;

@JsonDeserialize(using = ConversionDeserializer.class)
@Data
@EqualsAndHashCode
public class ConversionResponseX implements ConversionResponse {
    private Currency from;
    private Currency to;
    private BigDecimal rate;
    private BigDecimal amount;
    private String source;
}
