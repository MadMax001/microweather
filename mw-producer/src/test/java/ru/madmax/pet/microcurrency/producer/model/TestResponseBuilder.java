package ru.madmax.pet.microcurrency.producer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microweather.common.model.ConversionResponse;
import ru.madmax.pet.microweather.common.model.Currency;
import ru.madmax.pet.microweather.common.model.TestBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aResponse")
@With
public class TestResponseBuilder implements TestBuilder<ConversionResponseX> {

    private Currency from = Currency.USD;
    private Currency to = Currency.RUB;
    private BigDecimal rate = new BigDecimal("64.1824").setScale(4, RoundingMode.DOWN);
    private BigDecimal amount = new BigDecimal("155.8060").setScale(4, RoundingMode.DOWN);
    private String source = "http://www.test.ru";

    @Override
    public ConversionResponseX build() {
        var response = new ConversionResponseX();
        response.setFrom(from);
        response.setTo(to);;
        response.setRate(rate);
        response.setAmount(amount);
        response.setSource(source);
        return response;
    }
}
