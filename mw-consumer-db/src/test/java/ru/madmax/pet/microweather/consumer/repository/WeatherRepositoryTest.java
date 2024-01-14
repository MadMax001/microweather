package ru.madmax.pet.microweather.consumer.repository;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.UncategorizedR2dbcException;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.madmax.pet.microweather.consumer.configuration.ConsumerR2dbcConfiguration;
import ru.madmax.pet.microweather.consumer.model.TestWeatherDomain;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;

import static org.assertj.core.api.Assertions.*;

@DataR2dbcTest
@ActiveProfiles("test")
@Import({
        ConsumerR2dbcConfiguration.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherRepositoryTest {
    final WeatherRepository weatherRepository;

    @Test
    void saveWeather_AndFindIt() {
        WeatherDomain testWeather = TestWeatherDomain.aWeatherDomain()
                .withId("key_" + System.currentTimeMillis()).build();
        WeatherDomain weather = weatherRepository.save(testWeather).block();

        assertThat(weather).isNotNull();
        assertThat(weather.getId()).isEqualTo(testWeather.getId());

        Mono<WeatherDomain> weatherDBMono = weatherRepository.findById(weather.getId());
        StepVerifier.create(weatherDBMono)
                .assertNext(element -> {
                    assertThat(element.getId()).isEqualTo(weather.getId());
                    assertThat(element.getNow()).isEqualTo(weather.getNow());
/*
                    assertThat(element.getTemperature()).isEqualTo(weather.getTemperature(), withPrecision(2d));
                    assertThat(element.getWind()).isEqualTo(weather.getWind(), withPrecision(2d));
                    assertThat(element.getUrl()).isEqualTo(weather.getUrl());
*/

                    assertThat(element.getFact().getTemp()).isEqualTo(weather.getFact().getTemp(), withPrecision(2d));
                    assertThat(element.getFact().getWindSpeed()).isEqualTo(weather.getFact().getWindSpeed(), withPrecision(2d));
                    assertThat(element.getInfo().getUrl()).isEqualTo(weather.getInfo().getUrl());

                })
                .expectComplete()
                .verify();


    }

    @Test
    void saveTwoWeathers_withSameKeys_AndGetDuplicateKeyException() {
        WeatherDomain testWeather1 = TestWeatherDomain.aWeatherDomain()
                .withId("key_" + System.currentTimeMillis()).build();
        WeatherDomain testWeather2 = TestWeatherDomain.aWeatherDomain()
                .withId(testWeather1.getId()).build();

        var weather1Mono = weatherRepository.save(testWeather1);
        var weather2Mono = weatherRepository.save(testWeather2);
        weather1Mono.block();
        assertThatThrownBy(weather2Mono::block).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void saveWeather_withTooLongFields_InH2DB_AndGetUncategorizedR2dbcException() {
        WeatherDomain testWeather1 = TestWeatherDomain.aWeatherDomain()
                .withId("__________________________________________________________________________________key_"
                        + System.currentTimeMillis()).build();
        var weather1Mono = weatherRepository.save(testWeather1);
        assertThatThrownBy(weather1Mono::block).isInstanceOf(UncategorizedR2dbcException.class);

    }

}
