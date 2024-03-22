package ru.madmax.pet.microcurrency.currate.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microweather.common.model.Currency;
import ru.madmax.pet.microweather.common.model.TestBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aResponse")
@With
public class TestRemoteResponseBuilder implements TestBuilder<RemoteResponse> {
    public static final String MOCK_RESPONSE_JSON_VAL = "{\"status\":200,\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";

    private Currency from = Currency.USD;
    private Currency to = Currency.RUB;
    private BigDecimal rate = new BigDecimal("64.1824").setScale(4, RoundingMode.DOWN);

    @Override
    public RemoteResponse build() {
        var response = new RemoteResponse();
        response.setFrom(from);
        response.setTo(to);
        response.setRate(rate);
        return response;
    }
}
