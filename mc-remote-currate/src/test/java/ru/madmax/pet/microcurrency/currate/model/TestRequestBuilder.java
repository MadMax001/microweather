package ru.madmax.pet.microcurrency.currate.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microweather.common.model.ConversionRequest;
import ru.madmax.pet.microweather.common.model.Currency;
import ru.madmax.pet.microweather.common.model.TestBuilder;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aRequest")
@With
public class TestRequestBuilder implements TestBuilder<ConversionRequest> {
    private Currency base = Currency.RUB;
    private Currency convert = Currency.USD;
    private BigDecimal amount = new BigDecimal(50000);

    @Override
    public ConversionRequest build() {
        var request = new ConversionRequestX();
        request.setBase(base);
        request.setConvert(convert);
        request.setAmount(amount);
        return request;
    }
}
