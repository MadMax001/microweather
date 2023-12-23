package ru.madmax.pet.microweather.producer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import ru.madmax.pet.microweather.producer.configuration.HttpClientConfiguration;
import ru.madmax.pet.microweather.producer.model.RequestParams;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@TestPropertySource(properties = {"app.weather.timeout=1000"})
@ContextConfiguration(classes = HttpClientConfiguration.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")
class ReactRequestServiceTest {
    private final HttpClient httpClient;

    @MockBean
    LogService logService;

    MockWebServer remoteMockServer;
    WeatherRequestService loaderService;

    @Captor
    ArgumentCaptor<String> logInfoCaptor;

    @BeforeEach
    void initialize() throws IOException {
        remoteMockServer = new MockWebServer();
        remoteMockServer.start();

        loaderService = new ReactRequestService(httpClient, logService, 100, 1);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (remoteMockServer != null)
            remoteMockServer.shutdown();
    }

    @Test
    void sendRequest_AndCheckForRequestDetails_AndCheckForReturningMonoObject()
            throws JsonProcessingException, InterruptedException, MalformedURLException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final ObjectMapper mapper = new ObjectMapper();
        final String stringResponseContent = mapper.writeValueAsString(weather);

        doNothing().when(logService).info(any(String.class));

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

        final Point point = TestPointBuilder.aPoint().build();
        final String stringRequestContent = mapper.writeValueAsString(point);
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        Mono<Weather> weatherMono = loaderService.sendRequest(point, params);

        StepVerifier.create(weatherMono)
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
    void sendRequest_WhenServerIsUnavailableOnce_AndAnswersAfterOneRetry_CheckRetry_AndCheckForReturningMonoObject()
            throws JsonProcessingException, InterruptedException, MalformedURLException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final ObjectMapper mapper = new ObjectMapper();
        final String stringResponseContent = mapper.writeValueAsString(weather);

        doNothing().when(logService).info(any(String.class));

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

        final Point point = TestPointBuilder.aPoint().build();
        final String stringRequestContent = mapper.writeValueAsString(point);
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        Mono<Weather> weatherMono = loaderService.sendRequest(point, params);

        StepVerifier.create(weatherMono)
                .expectNext(weather)
                .expectComplete()
                .verify();

        verify(logService, times(4)).info(any(String.class));
        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getHeader("request-guid")).isEqualTo("test-guid");
            assertThat(recordedRequest.getRequestLine()).contains(url.getPath());
            assertThat(recordedRequest.getBody().toString()).contains(stringRequestContent);
        }
    }

    @Test
    void sendRequest_WhenServerIsUnavailableMoreThanRetryThreshold_CheckRetry_AndCheckForReturningMonoError()
            throws JsonProcessingException, InterruptedException, MalformedURLException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final ObjectMapper mapper = new ObjectMapper();
        final String stringResponseContent = mapper.writeValueAsString(weather);

        doNothing().when(logService).info(any(String.class));

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

        final Point point = TestPointBuilder.aPoint().build();
        final String stringRequestContent = mapper.writeValueAsString(point);
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();
        Mono<Weather> weatherMono = loaderService.sendRequest(point, params);
        StepVerifier.create(weatherMono)
                .expectErrorMatches(
                        throwable -> throwable.getClass().toString().contains("RetryExhaustedException") &&
                                throwable.getMessage().contains("Retries exhausted")
                ).verify();

        verify(logService, times(4)).info(any(String.class));
        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getHeader("request-guid")).isEqualTo("test-guid");
            assertThat(recordedRequest.getRequestLine()).contains(url.getPath());
            assertThat(recordedRequest.getBody().toString()).contains(stringRequestContent);
        }
    }

    @Test
    void sendRequest_AndCheckForLogOfHeadersAndStatusOfResponse() throws MalformedURLException {
        final String testHeaderKey = "request-test-header";
        final String testHeaderValue = "test-value";
        doNothing().when(logService).info(any(String.class));

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                if ("POST".equals(request.getMethod()) &&
                        request.getPath() != null &&
                        request.getPath().startsWith("/test-path")) {
                    return new MockResponse()
                            .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .addHeader(testHeaderKey, testHeaderValue)
                            .setBody("")
                            .setBodyDelay(100, TimeUnit.MILLISECONDS);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        remoteMockServer.setDispatcher(dispatcher);

        final Point point = TestPointBuilder.aPoint().build();
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        loaderService.sendRequest(point, params).block();
        verify(logService, times(2)).info(logInfoCaptor.capture());
        List<String> logInfoList = logInfoCaptor.getAllValues();
        assertThat(logInfoList.get(1)).contains(testHeaderKey, "200 OK", "test-guid");

    }

    @Test
    void sendRequest_OnNotExistingPage_andCheckForLogOfHeadersAndStatusOfResponse_andCheckForReturningMonoEmpty()
            throws MalformedURLException {

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                return new MockResponse().setResponseCode(404);
            }
        };

        remoteMockServer.setDispatcher(dispatcher);

        final Point point = TestPointBuilder.aPoint().build();
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        var weatherMono = loaderService.sendRequest(point, params);

        StepVerifier.create(weatherMono)
                .expectComplete()
                .verify();

        verify(logService, times(2)).info(logInfoCaptor.capture());
        List<String> logInfoList = logInfoCaptor.getAllValues();
        assertThat(logInfoList.get(1)).contains("404 NOT_FOUND", "test-guid");
    }

    @Test
    void sendRequest_andCheckForLogOfRequest()
            throws JsonProcessingException, MalformedURLException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final ObjectMapper mapper = new ObjectMapper();
        final String stringResponseContent = mapper.writeValueAsString(weather);

        doNothing().when(logService).info(any(String.class));

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

        final Point point = TestPointBuilder.aPoint().build();
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        loaderService.sendRequest(point, params).block();
        verify(logService, times(2)).info(logInfoCaptor.capture());
        List<String> logInfoList = logInfoCaptor.getAllValues();
        assertThat(logInfoList.get(0)).contains("test-guid", url.toString());

    }

    @Test
    void sendRequest_AndReceiveResponseWithDelayLessThanTimeout_AndCheckForReturningMonoObject()
            throws MalformedURLException, JsonProcessingException {
        doNothing().when(logService).info(any(String.class));
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final ObjectMapper mapper = new ObjectMapper();
        final String stringResponseContent = mapper.writeValueAsString(weather);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                if ("POST".equals(request.getMethod()) &&
                        request.getPath() != null &&
                        request.getPath().startsWith("/test-path")) {
                    return new MockResponse()
                            .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .setBody(stringResponseContent)
                            .setBodyDelay(500, TimeUnit.MILLISECONDS);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        remoteMockServer.setDispatcher(dispatcher);

        final Point point = TestPointBuilder.aPoint().build();
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();
        var startTime = System.nanoTime();
        var weatherMono = loaderService.sendRequest(point, params);
        var finishTime = System.nanoTime();
        var receiverWeather = weatherMono.block();
        var receivedTime = System.nanoTime();
        assertThat(receiverWeather).isEqualTo(weather);
        assertThat((int)((finishTime - startTime) / 1_000_000_000)).isZero();
        assertThat((int)(receivedTime - finishTime) / 500_000_000).isPositive();

    }

    @Test
    void sendRequest_AndReceiveResponseWithDelayMoreThanTimeout_AndCheckForReturningMonoObject()
            throws MalformedURLException, JsonProcessingException {
        doNothing().when(logService).info(any(String.class));
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final ObjectMapper mapper = new ObjectMapper();
        final String stringResponseContent = mapper.writeValueAsString(weather);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                if ("POST".equals(request.getMethod()) &&
                        request.getPath() != null &&
                        request.getPath().startsWith("/test-path")) {
                    return new MockResponse()
                            .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .setBody(stringResponseContent)
                            .setBodyDelay(1500, TimeUnit.MILLISECONDS);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        remoteMockServer.setDispatcher(dispatcher);

        final Point point = TestPointBuilder.aPoint().build();
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();
        var weatherMono = loaderService.sendRequest(point, params);
        StepVerifier.create(weatherMono)
                .expectErrorMatches(
                        throwable -> throwable.getClass().toString().contains("RetryExhaustedException") &&
                                throwable.getMessage().contains("Retries exhausted")
                ).verify();

    }
}