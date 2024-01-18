package ru.madmax.pet.microweather.consumer.service.converter.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.madmax.pet.microweather.common.model.TestWeatherBuilder;
import ru.madmax.pet.microweather.consumer.model.TestWeatherDomainBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherDomainConverterTest {
    WeatherDomainConverter weatherDomainConverter;

    @BeforeEach
    void setUp() {
        weatherDomainConverter = new WeatherDomainConverter();
    }

    @Test
    void checkConvert() {
        var weather = TestWeatherBuilder.aWeather().build();
        var exceptedWeatherDomain = TestWeatherDomainBuilder.aWeatherDomain()
                .withId("test-key")
                .build();

        var convertedWeatherDomain = weatherDomainConverter.convert("test-key", weather);
        assertThat(convertedWeatherDomain).isNotNull();
        assertThat(convertedWeatherDomain).usingRecursiveComparison().isEqualTo(exceptedWeatherDomain);
    }
}