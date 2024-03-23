package ru.madmax.pet.microcurrency.common.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

import java.math.BigDecimal;
import java.math.RoundingMode;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aConversion")
@With
public class TestConversionBuilder implements TestBuilder<Conversion> {

    private Currency base = Currency.RUB;
    private Currency convert = Currency.USD;
    private BigDecimal baseAmount = new BigDecimal(10000);
    private BigDecimal convertAmount = new BigDecimal("155.8060").setScale(4, RoundingMode.DOWN);
    private String source = "http://www.test.ru";

    @Override
    public Conversion build() {
        var conversion = new Conversion();
        conversion.setBase(base);
        conversion.setConvert(convert);
        conversion.setBaseAmount(baseAmount);
        conversion.setConversionAmount(convertAmount);
        conversion.setSource(source);
        return conversion;
    }
}
