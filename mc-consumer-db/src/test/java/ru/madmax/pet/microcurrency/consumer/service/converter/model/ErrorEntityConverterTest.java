package ru.madmax.pet.microcurrency.consumer.service.converter.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.madmax.pet.microcurrency.consumer.model.TestErrorDomainBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorEntityConverterTest {
    ErrorEntityConverter errorEntityConverter;

    @BeforeEach
    void setUp() {
        errorEntityConverter = new ErrorEntityConverter();
    }

    @Test
    void checkConvert() {
        var error = "Error presentation";
        var key = "error-key";
        var exceptedErrorDomain = TestErrorDomainBuilder.anErrorDomain().withId(key).withDetails(error).build();

        var convertedErrorDomain = errorEntityConverter.convert(key, error);
        assertThat(convertedErrorDomain).isNotNull();
        assertThat(convertedErrorDomain).usingRecursiveComparison().isEqualTo(exceptedErrorDomain);
    }
}