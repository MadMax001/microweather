package ru.madmax.pet.microweather.yandex.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;
import ru.madmax.pet.microweather.common.model.Point;
import ru.madmax.pet.microweather.common.model.TestPointBuilder;
import ru.madmax.pet.microweather.common.model.TestWeatherBuilder;
import ru.madmax.pet.microweather.common.model.Weather;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_GUID_KEY;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;


@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.weather.timeout=1000",
        "app.weather.url=http://localhost:44444",
        "app.weather.path=/test",
        "app.weather.retry.attempts=1"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class YandexServiceIT {
    private final WebTestClient webTestClient;
    MockWebServer remoteMockServer;
    String remoteYandexURL;
    final static String SERVICE_LOCAL_PATH = "/api/v1/weather";
    final static String GUID_HEADER_VALUE = "testguid";
    @BeforeEach
    void initialize() throws IOException {
        remoteMockServer = new MockWebServer();
        remoteMockServer.start(44444);
        remoteYandexURL = remoteMockServer.url("/test").toString();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (remoteMockServer != null)
            remoteMockServer.shutdown();
    }

    @Test
    void requestWeather_happyPass_CheckResponseAndHeaders() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final String stringContent = objectMapper.writeValueAsString(weather);

        final Point point = TestPointBuilder.aPoint().build();
        final String stringPoint = objectMapper.writeValueAsString(point);

        setMockResponseFromServer(500, stringContent);

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringPoint))
                .header(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .returnResult(Weather.class)
                .getResponseBody();

        StepVerifier.create(receivedResponseEntityContent)
                .expectNext(weather)
                .expectComplete()
                .verify();

    }

    @Test
    void requestWeather_ButRemoteServiceNotResponseDuringTimeoutValue_AndCheck500StatusAndErrorHeader() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final String stringContent = objectMapper.writeValueAsString(weather);

        final Point point = TestPointBuilder.aPoint().build();
        final String stringPoint = objectMapper.writeValueAsString(point);

        setMockResponseFromServer(1500, stringContent);

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringPoint))
                .header(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .expectHeader().exists(HEADER_REQUEST_ERROR_KEY)
                .returnResult(Void.class)
                .getResponseBody();

        StepVerifier.create(receivedResponseEntityContent)
                .expectComplete()
                .verify();

    }

    @Test
    void requestWeather_withWrongParams_AndCheck400StatusAndErrorHeader() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final String stringContent = objectMapper.writeValueAsString(weather);

        final String stringPoint = "{\"lon\":46.001373, \"lat\":\"51A\"}";

        setMockResponseFromServer(500, stringContent);

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringPoint))
                .header(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .expectHeader().exists(HEADER_REQUEST_ERROR_KEY)
                .returnResult(Void.class)
                .getResponseBody();

        StepVerifier.create(receivedResponseEntityContent)
                .expectComplete()
                .verify();

    }

    @Test
    void requestWeather_AndReceiveWrongStructureResponse_AndCheck500StatusAndErrorHeader()
            throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        final String stringContent = "5555555";

        final Point point = TestPointBuilder.aPoint().build();
        final String stringPoint = objectMapper.writeValueAsString(point);

        setMockResponseFromServer(500, stringContent);

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringPoint))
                .header(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY,
                        stringContainsInOrder("IllegalModelStructureException", stringContent))
                .returnResult(Void.class)
                .getResponseBody();

        StepVerifier.create(receivedResponseEntityContent)
                .expectComplete()
                .verify();
    }

    private void setMockResponseFromServer(int timeout, String responseContentString) {
        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                if ("GET".equals(request.getMethod()) &&
                        request.getPath() != null &&
                        request.getPath().startsWith("/test")) {
                    return new MockResponse()
                            .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .setBody(responseContentString)
                            .setBodyDelay(timeout, TimeUnit.MILLISECONDS);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        remoteMockServer.setDispatcher(dispatcher);

    }

}
