package ru.madmax.pet.microcurrency.currate.integration;

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
import ru.madmax.pet.microweather.common.model.Conversion;
import ru.madmax.pet.microweather.common.model.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_GUID_KEY;


@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.request.timeout=1000",
        "app.url=http://localhost:44444",
        "app.path=/test",
        "app.key=secret_key",
        "app.request.retry.attempts=1"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CurrateServiceIT {
    private final WebTestClient webTestClient;
    MockWebServer remoteMockServer;
    String remoteCurrateURL;
    final static String SERVICE_LOCAL_PATH = "/api/v1/convert";
    final static String GUID_HEADER_VALUE = "testguid";

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void initialize() throws IOException {
        remoteMockServer = new MockWebServer();
        remoteMockServer.start(44444);
        remoteCurrateURL = remoteMockServer.url("/test").toString();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (remoteMockServer != null)
            remoteMockServer.shutdown();
    }

    @Test
    void requestCurrency_happyPass_CheckResponseAndHeaders() throws JsonProcessingException {
        final ServiceRequest request = TestClientRequestBuilder.aRequest()
                .withBaseCurrency(Currency.RUB)
                .withConvertCurrency(Currency.USD)
                .withBaseAmount(new BigDecimal(10000))
                .build();
        final String stringRequest = objectMapper.writeValueAsString(request);

        final Conversion conversion = TestConversionBuilder.aConversion()
                .withBase(Currency.RUB)
                .withConvert(Currency.USD)
                .withBaseAmount(new BigDecimal(10000))
                .withConvertAmount(new BigDecimal("155.8060"))
                .withSource("http://localhost:44444")
                .build();
        //final String stringResponse = objectMapper.writeValueAsString(conversion);

        var remoteResponse = "{\"status\":200,\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";
        setMockResponseFromServer(500, remoteResponse);

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringRequest))
                .header(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, GUID_HEADER_VALUE)
                .returnResult(Conversion.class)
                .getResponseBody();

        StepVerifier.create(receivedResponseEntityContent)
                .expectNext(conversion)
                .expectComplete()
                .verify();

    }

    @Test
    void requestCurrency_ButRemoteServiceNotResponseDuringTimeoutValue_AndCheck500StatusAndErrorHeader() throws JsonProcessingException {
        final ServiceRequest request = TestClientRequestBuilder.aRequest()
                .withBaseCurrency(Currency.RUB)
                .withConvertCurrency(Currency.USD)
                .withBaseAmount(new BigDecimal(10000))
                .build();
        final String stringRequest = objectMapper.writeValueAsString(request);

        var remoteResponse = "{\"status\":200,\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";

        setMockResponseFromServer(1500, remoteResponse);

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringRequest))
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
    void requestCurrency_withWrongParams_AndCheck400StatusAndErrorHeader() throws JsonProcessingException {
        var remoteResponse = "{\"status\":200,\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";

        final String stringPoint = "{\"base_currency\":\"RUB\",\"convert_currency\":\"USDA\",\"base_amount\":10000}";

        setMockResponseFromServer(500, remoteResponse);

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
    void requestCurrency_AndReceiveWrongStructureResponse_AndCheck500StatusAndErrorHeader()
            throws JsonProcessingException {
        final String stringContent = "5555555";

        final ServiceRequest request = TestClientRequestBuilder.aRequest().build();
        final String stringPoint = objectMapper.writeValueAsString(request);

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
                .returnResult(Conversion.class)
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
