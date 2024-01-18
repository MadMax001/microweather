package ru.madmax.pet.microweather.consumer.service.converter.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.madmax.pet.microweather.consumer.model.TestErrorDomainBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorDomainConverterTest {
    ErrorDomainConverter errorDomainConverter;

    @BeforeEach
    void setUp() {
        errorDomainConverter = new ErrorDomainConverter();
    }

    @Test
    void checkConvert() {
        var error = "Error presentation";
        var key = "error-key";
        var exceptedErrorDomain = TestErrorDomainBuilder.anErrorDomain().withId(key).withDetails(error).build();

        var convertedErrorDomain = errorDomainConverter.convert(key, error);
        assertThat(convertedErrorDomain).isNotNull();
        assertThat(convertedErrorDomain).usingRecursiveComparison().isEqualTo(exceptedErrorDomain);
    }
}