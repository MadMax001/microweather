package ru.madmax.pet.microcurrency.currate.model;

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
public class TestResponseBuilder implements TestBuilder<ConversionResponse> {
    public static final String MOCK_RESPONSE_JSON_VAL = "{\"status\":200,\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";

    private Currency from = Currency.USD;
    private Currency to = Currency.RUB;
    private BigDecimal rate = new BigDecimal("64.1824").setScale(4, RoundingMode.DOWN);
    private BigDecimal amount = new BigDecimal("155.8060").setScale(4, RoundingMode.DOWN);
    private String source = "http://www.test.ru";

    @Override
    public ConversionResponse build() {
        var response = new ConversionResponseX();
        response.setFrom(from);
        response.setTo(to);;
        response.setRate(rate);
        response.setAmount(amount);
        response.setSource(source);
        return response;
    }
}
