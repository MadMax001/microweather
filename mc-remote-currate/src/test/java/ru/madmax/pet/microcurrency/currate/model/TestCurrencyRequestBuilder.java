package ru.madmax.pet.microcurrency.currate.model;

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
public class TestCurrencyRequestBuilder implements TestBuilder<CurrencyRequest> {
    private Currency baseCurrency = Currency.RUB;
    private Currency convertCurrency = Currency.USD;
    private BigDecimal baseAmount = new BigDecimal(50000);

    @Override
    public CurrencyRequest build() {
        var request = new CurrencyRequest();
        request.setBaseCurrency(baseCurrency);
        request.setConvertCurrency(convertCurrency);
        request.setBaseAmount(baseAmount);
        return request;
    }
}
