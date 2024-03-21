package ru.madmax.pet.microcurrency.producer.service;

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
import org.springframework.kafka.support.JacksonUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;
import ru.madmax.pet.microcurrency.producer.configuration.HttpClientConfiguration;
import ru.madmax.pet.microcurrency.producer.model.ConversionResponseX;
import ru.madmax.pet.microcurrency.producer.model.RequestParams;
import ru.madmax.pet.microcurrency.producer.model.TestCurrencyRequestXBuilder;
import ru.madmax.pet.microcurrency.producer.model.TestResponseBuilder;
import ru.madmax.pet.microweather.common.model.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_GUID_KEY;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@TestPropertySource(properties = {"app.request.timeout=1000"})
@ContextConfiguration(classes = {HttpClientConfiguration.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")
class ReactRequestServiceTest {
    private final HttpClient httpClient;
    ObjectMapper objectMapper = JacksonUtils.enhancedObjectMapper();
    @MockBean
    LogService logService;

    MockWebServer remoteMockServer;
    CurrencyRequestService loaderService;

    @Captor
    ArgumentCaptor<String> logInfoCaptor;

    @Captor
    ArgumentCaptor<String> errorInfoCaptor;

    @Captor
    ArgumentCaptor<String> keyCaptor;

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
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        final ConversionResponseX response = TestResponseBuilder.aResponse().build();
        final String stringResponseContent = objectMapper.writeValueAsString(response);
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

        final CurrencyRequest currencyRequest = TestCurrencyRequestXBuilder.aRequest().build();
        final String stringRequestContent = objectMapper.writeValueAsString(currencyRequest);

        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        Mono<ConversionResponseX> responseMono = loaderService.sendRequest(currencyRequest, params);

        StepVerifier.create(responseMono)
                .expectNext(response)
                .expectComplete()
                .verify();

        RecordedRequest request = remoteMockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader(HEADER_REQUEST_GUID_KEY)).isEqualTo("test-guid");
        assertThat(request.getRequestLine()).contains(url.getPath());
        assertThat(request.getBody().readUtf8()).isEqualTo(stringRequestContent);

        verify(logService, times(2)).info(keyCaptor.capture(), logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        List<String> keyValues = keyCaptor.getAllValues();

        assertThat(logLines.get(0)).contains("Send", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "status", "200 OK");
        assertThat(keyValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

        verify(logService, never()).error(anyString(), anyString());

    }

    @Test
    void sendRequest_WhenServerIs503UnavailableOnce_CheckRetry_AndAnswersAfterOneRetry_ReturnOK_AndCheckForReturningMonoObject_AndCheckLogs()
            throws JsonProcessingException, InterruptedException, MalformedURLException {
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        final ConversionResponseX response = TestResponseBuilder.aResponse().build();
        final String stringResponseContent = objectMapper.writeValueAsString(response);
        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

        final CurrencyRequest currencyRequest = TestCurrencyRequestXBuilder.aRequest().build();
        final String stringRequestContent = objectMapper.writeValueAsString(currencyRequest);
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        Mono<ConversionResponseX> responseMono = loaderService.sendRequest(currencyRequest, params);

        StepVerifier.create(responseMono)
                .expectNext(response)
                .expectComplete()
                .verify();

        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getHeader(HEADER_REQUEST_GUID_KEY)).isEqualTo("test-guid");
            assertThat(recordedRequest.getRequestLine()).contains(url.getPath());
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(stringRequestContent);
        }

        verify(logService, times(4)).info(keyCaptor.capture(), logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        List<String> keyValues = keyCaptor.getAllValues();

        assertThat(logLines.get(0)).contains("Send", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "status", "503 SERVICE_UNAVAILABLE");
        assertThat(logLines.get(2)).contains("Retrying, 0");
        assertThat(logLines.get(3)).contains("Response", "status", "200 OK");
        assertThat(keyValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

        verify(logService, times(1)).error(keyCaptor.capture(), errorInfoCaptor.capture());
        List<String> errorLines = errorInfoCaptor.getAllValues();
        List<String> keyErrorValues = keyCaptor.getAllValues();

        assertThat(errorLines.get(0)).contains("Error in response details", "503 Service Unavailable");
        assertThat(keyErrorValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

    }

    @Test
    void sendRequest_WhenServerIs503UnavailableMoreThanRetryThreshold_CheckRetry_ReturnError_AndCheckForReturningMonoError_AndCheckLogs()
            throws JsonProcessingException, InterruptedException, MalformedURLException {
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        final ConversionResponseX response = TestResponseBuilder.aResponse().build();
        final String stringResponseContent = objectMapper.writeValueAsString(response);
        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringResponseContent));

        final CurrencyRequest currencyRequest = TestCurrencyRequestXBuilder.aRequest().build();
        final String stringRequestContent = objectMapper.writeValueAsString(currencyRequest);
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        Mono<ConversionResponseX> responseMono = loaderService.sendRequest(currencyRequest, params);
        StepVerifier.create(responseMono)
                .expectErrorMatches(
                        throwable -> throwable.getClass().toString().contains("RetryExhaustedException") &&
                                throwable.getMessage().contains("Retries exhausted")
                ).verify();

        for (int i = 0; i < 2; i++) {
            RecordedRequest recordedRequest = remoteMockServer.takeRequest();
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getHeader(HEADER_REQUEST_GUID_KEY)).isEqualTo("test-guid");
            assertThat(recordedRequest.getRequestLine()).contains(url.getPath());
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(stringRequestContent);
        }

        verify(logService, times(4)).info(keyCaptor.capture(), logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        List<String> keyValues = keyCaptor.getAllValues();

        assertThat(logLines.get(0)).contains("Send", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "status", "503 SERVICE_UNAVAILABLE");
        assertThat(logLines.get(2)).contains("Retrying, 0");
        assertThat(logLines.get(3)).contains("Response", "status", "503 SERVICE_UNAVAILABLE");
        assertThat(keyValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

        verify(logService, times(2)).error(keyCaptor.capture(), errorInfoCaptor.capture());
        List<String> errorLines = errorInfoCaptor.getAllValues();
        List<String> keyErrorValues = keyCaptor.getAllValues();

        assertThat(errorLines.get(0)).contains("Error in response details", "503 Service Unavailable");
        assertThat(errorLines.get(1)).contains("Error in response details", "503 Service Unavailable");
        assertThat(keyErrorValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

    }

    @Test
    void sendRequest_Return500Error_AndCheckErrorResponseAndErrorHeader_AndCheckForHeaderAndStatusOfResponse_AndCheckLogs()
            throws MalformedURLException {
        final String testHeaderValue = "test-value";
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                if ("POST".equals(request.getMethod()) &&
                        request.getPath() != null &&
                        request.getPath().startsWith("/test-path")) {
                    return new MockResponse()
                            .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .addHeader(HEADER_REQUEST_ERROR_KEY, testHeaderValue)
                            .setBody("")
                            .setResponseCode(500)
                            .setBodyDelay(100, TimeUnit.MILLISECONDS);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        remoteMockServer.setDispatcher(dispatcher);

        final CurrencyRequest currencyRequest = TestCurrencyRequestXBuilder.aRequest().build();
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        var responseMono = loaderService.sendRequest(currencyRequest, params);

        StepVerifier.create(responseMono)
                .expectErrorMatches(
                        throwable -> throwable.getClass().toString().contains("RemoteServiceException") &&
                                throwable.getMessage().contains("test-value")
                ).verify();

        verify(logService, times(2)).info(keyCaptor.capture(), logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        List<String> keyValues = keyCaptor.getAllValues();

        assertThat(logLines.get(0)).contains("Send", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "status", "500 INTERNAL_SERVER_ERROR");
        assertThat(keyValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

        verify(logService, times(1)).error(keyCaptor.capture(), errorInfoCaptor.capture());
        List<String> errorLines = errorInfoCaptor.getAllValues();
        List<String> keyErrorValues = keyCaptor.getAllValues();

        assertThat(errorLines.get(0)).contains("Error in response details", "RemoteServiceException");
        assertThat(keyErrorValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

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

        final CurrencyRequest currencyRequest = TestCurrencyRequestXBuilder.aRequest().build();
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();

        var currencyMono = loaderService.sendRequest(currencyRequest, params);

        StepVerifier.create(currencyMono)
                .expectError()
                .verify();

        verify(logService, times(2)).info(keyCaptor.capture(), logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        List<String> keyValues = keyCaptor.getAllValues();

        assertThat(logLines.get(0)).contains("Send", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "status", "404 NOT_FOUND");
        assertThat(keyValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

        verify(logService, times(1)).error(keyCaptor.capture(), errorInfoCaptor.capture());
        List<String> errorLines = errorInfoCaptor.getAllValues();
        List<String> keyErrorValues = keyCaptor.getAllValues();

        assertThat(errorLines.get(0)).contains("Error in response details", "404 Not Found");
        assertThat(keyErrorValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

    }

    @Test
    void sendRequest_AndReceiveResponseWithDelayLessThanTimeout_ReturnOK_AndCheckForReturningMonoObject_AndCheckLogs()
            throws MalformedURLException, JsonProcessingException {
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        final ConversionResponseX response = TestResponseBuilder.aResponse().build();
        final String stringResponseContent = objectMapper.writeValueAsString(response);

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

        final CurrencyRequest currencyRequest = TestCurrencyRequestXBuilder.aRequest().build();
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();
        var startTime = System.nanoTime();
        var responseMono = loaderService.sendRequest(currencyRequest, params);
        var finishTime = System.nanoTime();
        var receiverCurrency = responseMono.block();
        var receivedTime = System.nanoTime();
        assertThat(receiverCurrency).isEqualTo(response);
        assertThat((int)((finishTime - startTime) / 1_000_000_000)).isZero();
        assertThat((int)(receivedTime - finishTime) / 500_000_000).isPositive();

        verify(logService, times(2)).info(keyCaptor.capture(), logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        List<String> keyValues = keyCaptor.getAllValues();

        assertThat(logLines.get(0)).contains("Send", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "status", "200 OK");
        assertThat(keyValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

    }

    @Test
    void sendRequest_AndReceiveResponseWithDelayMoreThanTimeout_ReturnError_AndCheckForReturningMonoObject_AndCheckLogs()
            throws MalformedURLException, JsonProcessingException {
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        final ConversionResponseX response = TestResponseBuilder.aResponse().build();
        final String stringResponseContent = objectMapper.writeValueAsString(response);

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

        final CurrencyRequest currencyRequest = TestCurrencyRequestXBuilder.aRequest().build();
        final URL url = new URL(remoteMockServer.url("/test-path").toString());
        final RequestParams params = RequestParams
                .builder()
                .guid("test-guid")
                .url(url)
                .build();
        var responseMono = loaderService.sendRequest(currencyRequest, params);
        StepVerifier.create(responseMono)
                .expectErrorMatches(
                        throwable -> throwable.getClass().toString().contains("RetryExhaustedException") &&
                                throwable.getMessage().contains("Retries exhausted")
                ).verify();

        verify(logService, times(4)).info(keyCaptor.capture(), logInfoCaptor.capture());
        List<String> logLines = logInfoCaptor.getAllValues();
        List<String> keyValues = keyCaptor.getAllValues();

        assertThat(logLines.get(0)).contains("Send", "/test-path");
        assertThat(logLines.get(1)).contains("Response", "status", "200 OK");
        assertThat(logLines.get(2)).contains("Retrying, 0");
        assertThat(logLines.get(3)).contains("Response", "status", "200 OK");
        assertThat(keyValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

        verify(logService, times(2)).error(keyCaptor.capture(), errorInfoCaptor.capture());
        List<String> errorLines = errorInfoCaptor.getAllValues();
        List<String> keyErrorValues = keyCaptor.getAllValues();

        assertThat(errorLines.get(0)).contains("Error in response details", "io.netty.handler.timeout.ReadTimeoutException");
        assertThat(errorLines.get(1)).contains("Error in response details", "io.netty.handler.timeout.ReadTimeoutException");
        assertThat(keyErrorValues).isNotEmpty().allMatch(key -> key.equals("test-guid"));

    }
}