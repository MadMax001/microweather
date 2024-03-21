package ru.madmax.pet.microcurrency.currate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.madmax.pet.microweather.common.model.ConversionRequest;
import ru.madmax.pet.microweather.common.model.Currency;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode
public class ConversionRequestX implements ConversionRequest {
    @JsonProperty("base_currency")
    @NotNull(message = "Base currency is wrong or not defined")
    private Currency base;

    @JsonProperty("convert_currency")
    @NotNull(message = "Currency for conversion is wrong or not defined")
    private Currency convert;

    @JsonProperty("base_amount")
    @NotNull(message = "Amount is not defined")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be more than 0")
    private BigDecimal amount;
}
