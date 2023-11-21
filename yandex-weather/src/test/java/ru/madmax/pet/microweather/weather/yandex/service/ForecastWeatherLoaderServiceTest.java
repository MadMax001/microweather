package ru.madmax.pet.microweather.weather.yandex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;
import ru.madmax.pet.microweather.weather.yandex.configuration.HttpClientConfiguration;
import ru.madmax.pet.microweather.weather.yandex.model.Point;
import ru.madmax.pet.microweather.weather.yandex.model.PointBuilder;
import ru.madmax.pet.microweather.weather.yandex.model.Weather;
import ru.madmax.pet.microweather.weather.yandex.model.WeatherBuilder;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"app.weather.timeout=1000"})
@ContextConfiguration(classes = HttpClientConfiguration.class)
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
        loaderService = new ForecastWeatherLoaderService(httpClient, token, url, 100);
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

    @Test
    void whenServerIsUnavailableTwice_CheckRetry() throws JsonProcessingException {
        final Weather weather = WeatherBuilder.aWeather().build();
        final String stringContent = new ObjectMapper().writeValueAsString(weather);

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringContent));

        Point point = PointBuilder.aPoint().build();

        Mono<Weather> monoWeather = loaderService.requestWeatherByPoint(point);



        StepVerifier.create(monoWeather)
                .expectNext(weather)
                .expectComplete()
                .verify(Duration.ofSeconds(3));

        assertThat(remoteMockServer.getRequestCount()).isEqualTo(2);

    }
}