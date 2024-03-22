package ru.madmax.pet.microcurrency.producer.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.madmax.pet.microweather.common.model.Currency;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode
public class RemoteConversionResponseX implements RemoteConversionResponse {
    private Currency from;
    private Currency to;
    private BigDecimal rate;
    private BigDecimal amount;
    private String source;
}
