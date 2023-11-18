package ru.madmax.pet.kafkatest.weather.yandex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;
import ru.madmax.pet.kafkatest.weather.yandex.model.Point;
import ru.madmax.pet.kafkatest.weather.yandex.model.PointBuilder;
import ru.madmax.pet.kafkatest.weather.yandex.model.Weather;
import ru.madmax.pet.kafkatest.weather.yandex.model.WeatherBuilder;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest                                                     //todo может что-то более "легкое"
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")
class ForecastWeatherLoaderServiceTest {
    private final HttpClient httpClient;


    static MockWebServer remoteMockServer;
    WeatherLoaderService loaderService;


    @BeforeAll
    static void setUp() throws IOException {
        remoteMockServer = new MockWebServer();
        remoteMockServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (remoteMockServer != null)
            remoteMockServer.shutdown();
    }

    @BeforeEach
    void initialize() {
        String url = remoteMockServer.url("").toString();
        String token = "test-token";
        loaderService = new ForecastWeatherLoaderService(httpClient, token, url);
    }

    @Test
    void checkForRemoteRequest() throws JsonProcessingException, InterruptedException {
        final Weather weather = WeatherBuilder.aWeather().build();
        final String stringContent = new ObjectMapper().writeValueAsString(weather);

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringContent));

        Point point = PointBuilder.aPoint().build();

        Mono<Weather> monoWeather = loaderService.requestWeatherByPoint(point);



        StepVerifier.create(monoWeather)
                .expectNext(weather)
                .expectComplete()
                .verify(Duration.ofSeconds(3));

        assertThat(remoteMockServer.getRequestCount()).isEqualTo(1);

        RecordedRequest request = remoteMockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getHeader("X-Yandex-API-Key")).isEqualTo("test-token");
        assertThat(request.getRequestLine()).contains(
                point.getLat().toString(),
                point.getLon().toString(),
                "lat=",
                "lon=",
                "forecast");

    }
}