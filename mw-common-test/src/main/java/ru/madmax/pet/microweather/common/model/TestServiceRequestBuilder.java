package ru.madmax.pet.microweather.common.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aRequest")
@With
public class TestServiceRequestBuilder implements TestBuilder<ServiceRequest> {
    private Currency baseCurrency = Currency.RUB;
    private Currency convertCurrency = Currency.USD;
    private BigDecimal baseAmount = new BigDecimal(50000);

    @Override
    public ServiceRequest build() {
        var request = new ServiceRequest();
        request.setBaseCurrency(baseCurrency);
        request.setConvertCurrency(convertCurrency);
        request.setBaseAmount(baseAmount);
        return request;
    }
}
