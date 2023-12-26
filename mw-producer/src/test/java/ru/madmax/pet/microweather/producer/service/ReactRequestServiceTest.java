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
    void sendRequest_ReturnOK_AndCheckForRequestDetails_AndCheckForReturningMonoObject_AndCheckLogs()
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

        verify(logService, times(2)).info(logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        assertThat(logLines.get(0)).contains("Send", "test-guid", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "test-guid", "status", "200 OK");

    }

    @Test
    void sendRequest_WhenServerIs503UnavailableOnce_CheckRetry_AndAnswersAfterOneRetry_ReturnOK_AndCheckForReturningMonoObject_AndCheckLogs()
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

        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getHeader("request-guid")).isEqualTo("test-guid");
            assertThat(recordedRequest.getRequestLine()).contains(url.getPath());
            assertThat(recordedRequest.getBody().toString()).contains(stringRequestContent);
        }

        verify(logService, times(5)).info(logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        assertThat(logLines.get(0)).contains("Send", "test-guid", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "test-guid", "status", "503 SERVICE_UNAVAILABLE");
        assertThat(logLines.get(2)).contains("Error in response details", "503 Service Unavailable");
        assertThat(logLines.get(3)).contains("Retrying, 0");
        assertThat(logLines.get(4)).contains("Response", "test-guid", "status", "200 OK");
    }

    @Test
    void sendRequest_WhenServerIs503UnavailableMoreThanRetryThreshold_CheckRetry_ReturnError_AndCheckForReturningMonoError_AndCheckLogs()
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

        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getHeader("request-guid")).isEqualTo("test-guid");
            assertThat(recordedRequest.getRequestLine()).contains(url.getPath());
            assertThat(recordedRequest.getBody().toString()).contains(stringRequestContent);
        }

        verify(logService, times(6)).info(logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        assertThat(logLines.get(0)).contains("Send", "test-guid", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "test-guid", "status", "503 SERVICE_UNAVAILABLE");
        assertThat(logLines.get(2)).contains("Error in response details", "503 Service Unavailable");
        assertThat(logLines.get(3)).contains("Retrying, 0");
        assertThat(logLines.get(4)).contains("Response", "test-guid", "status", "503 SERVICE_UNAVAILABLE");
        assertThat(logLines.get(5)).contains("Error in response details", "503 Service Unavailable");

    }

    @Test
    void sendRequest_Return500Error_AndCheckErrorResponseAndErrorHeader_AndCheckForHeaderAndStatusOfResponse_AndCheckLogs()
            throws MalformedURLException {
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
                            .setResponseCode(500)
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

        var weatherMono = loaderService.sendRequest(point, params);

        StepVerifier.create(weatherMono)
                .expectError()
                .verify();

        verify(logService, times(3)).info(logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        assertThat(logLines.get(0)).contains("Send", "test-guid", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "test-guid", "status", "500 INTERNAL_SERVER_ERROR");
        assertThat(logLines.get(2)).contains("Error in response details", "500 Internal Server Error");
    }

    @Test
    void sendRequestOn404NotExistingPage_ReturnError_andCheckForLogOfHeadersAndStatusOfResponse_andCheckForReturningMonoEmpty_AndCheckLogs()
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
                .expectError()
                .verify();

        verify(logService, times(3)).info(logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        assertThat(logLines.get(0)).contains("Send", "test-guid", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "test-guid", "status", "404 NOT_FOUND");
        assertThat(logLines.get(2)).contains("Error in response details", "404 Not Found");
    }

    @Test
    void sendRequest_AndReceiveResponseWithDelayLessThanTimeout_ReturnOK_AndCheckForReturningMonoObject_AndCheckLogs()
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

        verify(logService, times(2)).info(logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        assertThat(logLines.get(0)).contains("Send", "test-guid", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "test-guid", "status", "200 OK");


    }

    @Test
    void sendRequest_AndReceiveResponseWithDelayMoreThanTimeout_ReturnError_AndCheckForReturningMonoObject_AndCheckLogs()
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

        verify(logService, times(6)).info(logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        assertThat(logLines.get(0)).contains("Send", "test-guid", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "test-guid", "status", "200 OK");          //todo Timeout!!!
        assertThat(logLines.get(2)).contains("Error in response details", "io.netty.handler.timeout.ReadTimeoutException");
        assertThat(logLines.get(3)).contains("Retrying, 0");
        assertThat(logLines.get(1)).contains("Response", "test-guid", "status", "200 OK");          //todo Timeout!!!
        assertThat(logLines.get(5)).contains("Error in response details", "io.netty.handler.timeout.ReadTimeoutException");
    }
}