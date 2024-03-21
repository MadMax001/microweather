package ru.madmax.pet.microweather.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CurrencyRequest {
    @JsonProperty("base_currency")
    private Currency baseCurrency;

    @JsonProperty("convert_currency")
    private Currency convertCurrency;

    @JsonProperty("base_amount")
    private BigDecimal baseAmount;
}
