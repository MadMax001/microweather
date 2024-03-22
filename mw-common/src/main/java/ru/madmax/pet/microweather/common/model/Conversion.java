package ru.madmax.pet.microweather.common.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode
public class Conversion {
    private Currency base;
    private Currency convert;
    private BigDecimal baseAmount;
    private BigDecimal conversionAmount;
    private String source;

    @Override
    public String toString() {
        return this.getSource() +
                " {" +
                this.getBase().name() +
                " -> " +
                this.getConvert().name() +
                ": " +
                this.getBaseAmount().toPlainString() +
                " -> " +
                this.getConversionAmount().toPlainString() +
                "}";
    }
}
