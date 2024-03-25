package ru.madmax.pet.microcurrency.consumer.service.converter.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.madmax.pet.microcurrency.consumer.model.TestConversionEntityBuilder;
import ru.madmax.pet.microcurrency.common.model.TestConversionBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionEntityConverterTest {
    ConversionEntityConverter conversionEntityConverter;

    @BeforeEach
    void setUp() {
        conversionEntityConverter = new ConversionEntityConverter();
    }

    @Test
    void checkConvert() {
        var conversion = TestConversionBuilder.aConversion().build();
        var exceptedConversionEntity = TestConversionEntityBuilder.aConversionEntity()
                .withId("test-key")
                .build();

        var convertedConversionDomain = conversionEntityConverter.convert("test-key", conversion);
        assertThat(convertedConversionDomain).isNotNull();

        assertThat(convertedConversionDomain.getId()).isEqualTo(exceptedConversionEntity.getId());
        assertThat(convertedConversionDomain.getBase()).isEqualTo(exceptedConversionEntity.getBase());
        assertThat(convertedConversionDomain.getConvert()).isEqualTo(exceptedConversionEntity.getConvert());
        assertThat(convertedConversionDomain.getBaseAmount()).isEqualTo(exceptedConversionEntity.getBaseAmount());
        assertThat(convertedConversionDomain.getConversionAmount()).isEqualTo(exceptedConversionEntity.getConversionAmount());
        assertThat(convertedConversionDomain.getSourceId()).isNull();

    }
}