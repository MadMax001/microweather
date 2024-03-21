package ru.madmax.pet.microcurrency.producer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microweather.common.model.Currency;
import ru.madmax.pet.microweather.common.model.CurrencyRequest;
import ru.madmax.pet.microweather.common.model.TestBuilder;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aRequest")
@With
public class TestCurrencyRequestXBuilder implements TestBuilder<CurrencyRequest> {
    private Currency baseCurrency = Currency.RUB;;
    private Currency convertCurrency = Currency.USD;
    private BigDecimal baseAmount = new BigDecimal(50000);
    private String source = "www.site1.ru";

    @Override
    public CurrencyRequest build() {
        var request = new CurrencyRequestX();
        request.setBaseCurrency(baseCurrency);
        request.setConvertCurrency(convertCurrency);
        request.setBaseAmount(baseAmount);
        request.setSource(source);
        return request;
    }
}