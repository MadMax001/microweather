package ru.madmax.pet.microcurrency.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode
public class ServiceRequest {
    @JsonProperty("base_currency")
    @NotNull(message = "Base currency is wrong or not defined")
    private Currency baseCurrency;

    @JsonProperty("convert_currency")
    @NotNull(message = "Currency for conversion is wrong or not defined")
    private Currency convertCurrency;

    @JsonProperty("base_amount")
    @NotNull(message = "Amount is not defined")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be more than 0")
    private BigDecimal baseAmount;


    @Override
    public String toString() {
        return  "{" +
                    this.getBaseCurrency().name() +
                    " -> " +
                    this.getConvertCurrency().name() +
                    ", " + this.getBaseAmount().toPlainString() +
                "}";
    }
}
