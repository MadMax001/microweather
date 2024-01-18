package ru.madmax.pet.microweather.consumer.repository;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.BadSqlGrammarException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.madmax.pet.microweather.consumer.AbstractContainersIntegrationTest;
import ru.madmax.pet.microweather.consumer.model.TestWeatherDomainBuilder;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataR2dbcTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherRepositoryContainersTest extends AbstractContainersIntegrationTest {
    final WeatherRepository weatherRepository;

    @Test
    void saveWeather_AndFindIt() {
        WeatherDomain testWeather = TestWeatherDomainBuilder.aWeatherDomain()
                .withId("key_" + System.currentTimeMillis()).build();
        WeatherDomain weather = weatherRepository.save(testWeather).block();

        assertThat(weather).isNotNull();
        assertThat(weather.getId()).isEqualTo(testWeather.getId());

        Mono<WeatherDomain> weatherDBMono = weatherRepository.findById(weather.getId());
        StepVerifier.create(weatherDBMono)
                .assertNext(element -> {
                    assertThat(element.getId()).isEqualTo(weather.getId());
                    assertThat(element.getNow()).isEqualTo(weather.getNow());
                    assertThat(element.getTemperature()).isEqualTo(weather.getTemperature(), withPrecision(2d));
                    assertThat(element.getWind()).isEqualTo(weather.getWind(), withPrecision(2d));
                    assertThat(element.getUrl()).isEqualTo(weather.getUrl());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void saveTwoWeathers_withSameKeys_AndGetDuplicateKeyException() {
        WeatherDomain testWeather1 = TestWeatherDomainBuilder.aWeatherDomain()
                .withId("key_" + System.currentTimeMillis()).build();
        WeatherDomain testWeather2 = TestWeatherDomainBuilder.aWeatherDomain()
                .withId(testWeather1.getId()).build();

        var weather1Mono = weatherRepository.save(testWeather1);
        var weather2Mono = weatherRepository.save(testWeather2);
        weather1Mono.block();
        assertThatThrownBy(weather2Mono::block).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void saveWeather_withTooLongFields_InPostgreSQLDB_AndGetBadSqlGrammarException() {
        WeatherDomain testWeather1 = TestWeatherDomainBuilder.aWeatherDomain()
                .withId("__________________________________________________________________________________key_"
                        + System.currentTimeMillis()).build();
        var weather1Mono = weatherRepository.save(testWeather1);
        assertThatThrownBy(weather1Mono::block).isInstanceOf(BadSqlGrammarException.class);
    }
}