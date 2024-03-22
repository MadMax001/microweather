package ru.madmax.pet.microcurrency.producer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microweather.common.model.Currency;
import ru.madmax.pet.microweather.common.model.TestBuilder;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aRequest")
@With
public class TestClientRequestBuilder implements TestBuilder<ClientRequest> {
    private Currency baseCurrency = Currency.RUB;
    private Currency convertCurrency = Currency.USD;
    private BigDecimal baseAmount = new BigDecimal(50000);
    private String source = "first";

    @Override
    public ClientRequest build() {
        var request = new ClientRequest();
        request.setBaseCurrency(baseCurrency);
        request.setConvertCurrency(convertCurrency);
        request.setBaseAmount(baseAmount);
        request.setSource(source);
        return request;
    }
}
