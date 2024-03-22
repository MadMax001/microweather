package ru.madmax.pet.microcurrency.currate.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.madmax.pet.microcurrency.currate.misc.RemoteConversionDeserializer;
import ru.madmax.pet.microweather.common.model.Currency;

import java.math.BigDecimal;

@JsonDeserialize(using = RemoteConversionDeserializer.class)
@Data
@EqualsAndHashCode
public class RemoteResponse {
    private Currency from;
    private Currency to;
    private BigDecimal rate;
}
