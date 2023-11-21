package ru.madmax.pet.microweather.producer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
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
import ru.madmax.pet.microweather.producer.configuration.HttpClientConfiguration;
import ru.madmax.pet.microweather.producer.model.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"app.weather.timeout=1000"})
@ContextConfiguration(classes = HttpClientConfiguration.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")

class ReactRequestServiceTest {
    private final HttpClient httpClient;

    MockWebServer remoteMockServer;
    WeatherRequestService loaderService;

    @BeforeEach
    void initialize() throws IOException {
        remoteMockServer = new MockWebServer();
        remoteMockServer.start();
        loaderService = new ReactRequestService(httpClient, 100, 1);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (remoteMockServer != null)
            remoteMockServer.shutdown();
    }

    @Test
    void checkForRemoteRequest_withoutCheckingGuidHeaderInResponse()
            throws JsonProcessingException, InterruptedException, MalformedURLException {
        final Weather weather = WeatherBuilder.aWeather().build();
        final ObjectMapper mapper = new ObjectMapper();
        final String stringResponseContent = mapper.writeValueAsString(weather);

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

        final Point point = PointBuilder.aPoint().build();
        final String stringRequestContent = mapper.writeValueAsString(point);
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        Mono<Weather> monoWeather = loaderService.registerRequest(point, params);

        StepVerifier.create(monoWeather)
                .expectNext(weather)
                .expectComplete()
                .verify();

        RecordedRequest request = remoteMockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader("request-guid")).isEqualTo("test-guid");
        assertThat(request.getRequestLine()).contains(url.getPath());
        assertThat(request.getBody().toString()).contains(stringRequestContent);

    }

   @Test
    void whenServerIsUnavailableOnce_AndAnswerAfterOneRetry_CheckRetry()
           throws JsonProcessingException, InterruptedException, MalformedURLException {
       final Weather weather = WeatherBuilder.aWeather().build();
       final ObjectMapper mapper = new ObjectMapper();
       final String stringResponseContent = mapper.writeValueAsString(weather);

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

       final Point point = PointBuilder.aPoint().build();
       final String stringRequestContent = mapper.writeValueAsString(point);
       final URL url = new URL(remoteMockServer.url("/test-path").toString());
       final RequestParams params = RequestParams
               .builder()
               .guid("test-guid")
               .url(url)
               .build();

       Mono<Weather> monoWeather = loaderService.registerRequest(point, params);

        StepVerifier.create(monoWeather)
                .expectNext(weather)
                .expectComplete()
                .verify();


        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getHeader("request-guid")).isEqualTo("test-guid");
            assertThat(recordedRequest.getRequestLine()).contains(url.getPath());
            assertThat(recordedRequest.getBody().toString()).contains(stringRequestContent);
        }
    }

    @Test
    void whenServerIsUnavailable2Times_AndAnswerAfterOneRetry_CheckRetry_MaxRetryAttemptsExceed()
            throws JsonProcessingException, InterruptedException, MalformedURLException {
        final Weather weather = WeatherBuilder.aWeather().build();
        final ObjectMapper mapper = new ObjectMapper();
        final String stringResponseContent = mapper.writeValueAsString(weather);

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

        final Point point = PointBuilder.aPoint().build();
        final String stringRequestContent = mapper.writeValueAsString(point);
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();
        Mono<Weather> monoWeather = loaderService.registerRequest(point, params);

        StepVerifier.create(monoWeather)
                .expectErrorMatches(
                        throwable -> throwable.getClass().toString().contains("RetryExhaustedException") &&
                                throwable.getMessage().contains("Retries exhausted")
                ).verify();


        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getHeader("request-guid")).isEqualTo("test-guid");
            assertThat(recordedRequest.getRequestLine()).contains(url.getPath());
            assertThat(recordedRequest.getBody().toString()).contains(stringRequestContent);
        }
    }
}