package ru.madmax.pet.microcurrency.currate.misc;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.madmax.pet.microcurrency.currate.model.ConversionResponseX;
import ru.madmax.pet.microcurrency.currate.model.TestResponseBuilder;
import ru.madmax.pet.microweather.common.model.ConversionResponse;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ConversionDeserializerTest {
    final JacksonTester<ConversionResponseX> json;

    @Test
    void testDeserialization() throws IOException {
        ConversionResponse expected = TestResponseBuilder.aResponse().build();
        var convertedResponse = json.parse(TestResponseBuilder.MOCK_RESPONSE_JSON_VAL).getObject();
        assertThat(convertedResponse.getFrom()).isEqualTo(expected.getFrom());
        assertThat(convertedResponse.getTo()).isEqualTo(expected.getTo());
        assertThat(convertedResponse.getRate())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(expected.getRate());
    }
}