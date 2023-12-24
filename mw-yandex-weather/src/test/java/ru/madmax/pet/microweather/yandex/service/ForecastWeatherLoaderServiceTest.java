package ru.madmax.pet.microweather.yandex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
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
import ru.madmax.pet.microweather.common.model.Point;
import ru.madmax.pet.microweather.common.model.TestPointBuilder;
import ru.madmax.pet.microweather.common.model.TestWeatherBuilder;
import ru.madmax.pet.microweather.common.model.Weather;
import ru.madmax.pet.microweather.yandex.configuration.HttpClientConfiguration;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"app.weather.timeout=1000"})
@ContextConfiguration(classes = HttpClientConfiguration.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")
class ForecastWeatherLoaderServiceTest {
    final HttpClient httpClient;
    ObjectMapper objectMapper = new ObjectMapper();

    MockWebServer remoteMockServer;
    WeatherLoaderService loaderService;

    @BeforeEach
    void initialize() throws IOException {
        remoteMockServer = new MockWebServer();
        remoteMockServer.start();
        String url = remoteMockServer.url("").toString();
        String token = "test-token";
        loaderService = new ForecastWeatherLoaderService(httpClient, objectMapper, token, url, "", 100, 1);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (remoteMockServer != null)
            remoteMockServer.shutdown();
    }

    @Test
    void requestWeather_AndCheckRequestWithHeader_AndCheckResponse() throws JsonProcessingException, InterruptedException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final String stringContent = new ObjectMapper().writeValueAsString(weather);

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringContent));

        Point point = TestPointBuilder.aPoint().build();

        Mono<Weather> monoWeather = loaderService.requestWeatherByPoint(point);

        StepVerifier.create(monoWeather)
                .expectNext(weather)
                .expectComplete()
                .verify();

        RecordedRequest request = remoteMockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getHeader("X-Yandex-API-Key")).isEqualTo("test-token");
        assertThat(request.getRequestLine()).contains(
                point.getLat().toString(),
                point.getLon().toString(),
                "lat=",
                "lon=");

    }

    @Test
    void requestWeather_AndReceiveIllegalModelResponse_ThrowsIllegalModelStructureException() {
        final String stringContent = "1234!!";

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringContent));

        Point point = TestPointBuilder.aPoint().build();

        Mono<Weather> monoWeather = loaderService.requestWeatherByPoint(point);

        StepVerifier.create(monoWeather)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("1234!!")
                )
                .verify();

    }

    @Test
    void whenServerIsUnavailableOnce_AndAnswerAfterOneRetry_CheckRetry() throws JsonProcessingException, InterruptedException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final String stringContent = new ObjectMapper().writeValueAsString(weather);

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringContent));

        Point point = TestPointBuilder.aPoint().build();
        Mono<Weather> monoWeather = loaderService.requestWeatherByPoint(point);

        StepVerifier.create(monoWeather)
                .expectNext(weather)
                .expectComplete()
                .verify();


        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getRequestLine()).contains(
                    point.getLat().toString(),
                    point.getLon().toString(),
                    "lat=",
                    "lon=");
        }
    }

    @Test
    void whenServerIsUnavailable2Times_AndAnswerAfterOneRetry_CheckRetry_MaxRetryAttemptsExceed() throws JsonProcessingException, InterruptedException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final String stringContent = new ObjectMapper().writeValueAsString(weather);

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringContent));

        Point point = TestPointBuilder.aPoint().build();
        Mono<Weather> monoWeather = loaderService.requestWeatherByPoint(point);

        StepVerifier.create(monoWeather)
                .expectErrorMatches(
                        throwable -> throwable.getClass().toString().contains("RetryExhaustedException") &&
                        throwable.getMessage().contains("Retries exhausted")
                ).verify();


        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            assertThat(recordedRequest.getRequestLine()).contains(
                    point.getLat().toString(),
                    point.getLon().toString(),
                    "lat=",
                    "lon=");
        }
    }

}