package ru.madmax.pet.microcurrency.currate.service;

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
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;
import ru.madmax.pet.microcurrency.common.model.*;
import ru.madmax.pet.microcurrency.currate.configuration.HttpClientConfiguration;
import ru.madmax.pet.microcurrency.currate.configuration.MainConfig;
import ru.madmax.pet.microcurrency.currate.exception.IllegalAmountException;
import ru.madmax.pet.microcurrency.currate.exception.IllegalRateException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"app.request.timeout=1000"})
@ContextConfiguration(classes = {
        HttpClientConfiguration.class,
        MainConfig.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")
class CurrateCurrencyReactServiceTest {


    final HttpClient httpClient;
    final ObjectMapper objectMapper;
    
    @Mock
    ConversionService conversionService;

    MockWebServer remoteMockServer;
    CurrencyService currencyService;
    String token = "test-token";
    String host;

    @BeforeEach
    void initialize() throws IOException {
        remoteMockServer = new MockWebServer();
        remoteMockServer.start();
        host = remoteMockServer.url("").toString();
        currencyService = new CurrateCurrencyService(
                httpClient, conversionService, objectMapper, token, host, "", 100, 1);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (remoteMockServer != null)
            remoteMockServer.shutdown();
    }

    @Test
    void requestCurrency_AndCheckRequestWithHeader_AndCheckResponse() throws InterruptedException, IllegalAmountException, IllegalRateException {
        final ServiceRequest request = TestServiceRequestBuilder.aRequest()
                .withBaseCurrency(Currency.RUB)
                .withConvertCurrency(Currency.USD)
                .withBaseAmount(new BigDecimal(10000))
                .build();

        var conversionResult = BigDecimal.ONE;
        final Conversion conversion = TestConversionBuilder.aConversion()
                .withSource(host)
                .withBase(Currency.RUB)
                .withConvert(Currency.USD)
                .withBaseAmount(new BigDecimal(10000))
                .withConvertAmount(conversionResult)
                .build();

        final String responseStr = "{\"status\":200,\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";

        when(conversionService.covert(any(), any())).thenReturn(conversionResult);
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectNext(conversion)
                .expectComplete()
                .verify();

        RecordedRequest mockRequest = remoteMockServer.takeRequest();
        assertThat(mockRequest.getMethod()).isEqualTo("GET");
        assertThat(mockRequest.getRequestLine()).contains(
                request.getConvertCurrency().name() + request.getBaseCurrency().name(),
                token);
    }

    @Test
    void requestCurrency_AndReceiveIllegalModelResponse_ThrowsIllegalModelStructureException() {
        final String stringContent = "1234!!";

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(stringContent));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("1234!!")
                )
                .verify();

    }

    @Test
    void requestCurrency_AndCodeNotEquals200_ThrowsIllegalModelStructureException() {
        final String responseStr = " {\"status\":\"500\",\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("Wrong answer code") &&
                        throwable.getMessage().contains("500")
                )
                .verify();

    }

    @Test
    void requestCurrency_AndNotNumericCode_ThrowsIllegalModelStructureException() {
        final String responseStr = " {\"status\":\"aaa\",\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("Wrong status") &&
                        throwable.getMessage().contains("aaa")
                )
                .verify();

    }

    @Test
    void requestCurrency_AndNoStatusCode_ThrowsIllegalModelStructureException() {
        final String responseStr = " {\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("Wrong status") &&
                        throwable.getMessage().contains(responseStr)
                )
                .verify();

    }

    @Test
    void requestCurrency_AndNoDataBlock_ThrowsIllegalModelStructureException() {
        final String responseStr = " {\"status\":\"200\",\"message\":\"rates\"}";

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("Empty data") &&
                        throwable.getMessage().contains(responseStr)
                )
                .verify();

    }

    @Test
    void requestCurrency_AndEmptyDataBlock_ThrowsIllegalModelStructureException() {
        final String responseStr = " {\"status\":\"200\",\"message\":\"rates\",\"data\":{}}";

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("Empty data") &&
                        throwable.getMessage().contains(responseStr)
                )
                .verify();

    }

    @Test
    void requestCurrency_WithNonNumericRateInDataBlock_ThrowsIllegalModelStructureException() {
        final String responseStr = " {\"status\":\"200\",\"message\":\"rates\",\"data\":{\"USDRUB\":\"abc\"}}";
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("Illegal rate") &&
                        throwable.getMessage().contains("abc")
                )
                .verify();
    }

    @Test
    void requestCurrency_WithIllegalCurrencyPair_ThrowsIllegalModelStructureException() {
        final String responseStr = " {\"status\":\"200\",\"message\":\"rates\",\"data\":{\"GELID\":\"12.3456\"}}";
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("Illegal currency pair") &&
                        throwable.getMessage().contains("GELID")
                )
                .verify();
    }

    @Test
    void requestCurrency_WithNonRegisteredCurrency_ThrowsIllegalModelStructureException() {
        final String responseStr = " {\"status\":\"200\",\"message\":\"rates\",\"data\":{\"GELIDI\":\"12.3456\"}}";
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> throwable.getClass().toString().contains("IllegalModelStructureException") &&
                        throwable.getMessage().contains("Non registered currency code") &&
                        throwable.getMessage().contains("GEL")
                )
                .verify();
    }


    @Test
    void whenServerIsUnavailableOnce_AndAnswerAfterOneRetry_CheckRetry() throws InterruptedException, IllegalAmountException, IllegalRateException {
        final String responseStr = " {\"status\":\"200\",\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";
        var conversionResult = BigDecimal.ONE;
        final Conversion conversion = TestConversionBuilder.aConversion()
                .withSource(host)
                .withBase(Currency.RUB)
                .withConvert(Currency.USD)
                .withBaseAmount(new BigDecimal(50000))
                .withConvertAmount(conversionResult)
                .build();

        when(conversionService.covert(any(), any())).thenReturn(conversionResult);

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectNext(conversion)
                .expectComplete()
                .verify();

        for (int i = 0; i < 2; i++) {
            RecordedRequest mockRequest = remoteMockServer.takeRequest();
            assertThat(mockRequest.getMethod()).isEqualTo("GET");
            assertThat(mockRequest.getRequestLine()).contains(
                    request.getConvertCurrency().name() + request.getBaseCurrency().name(),
                    token);
        }

    }

    @Test
    void firstServerAnswerAfterTimeout_AndSecondAnswerInTime_CheckRetry() throws InterruptedException, IllegalAmountException, IllegalRateException {
        final String responseStr = " {\"status\":\"200\",\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";
        var conversionResult = BigDecimal.ONE;
        final Conversion conversion = TestConversionBuilder.aConversion()
                .withSource(host)
                .withBase(Currency.RUB)
                .withConvert(Currency.USD)
                .withBaseAmount(new BigDecimal(50000))
                .withConvertAmount(conversionResult)
                .build();

        when(conversionService.covert(any(), any())).thenReturn(conversionResult);

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr).setBodyDelay(1500, TimeUnit.MILLISECONDS));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr).setBodyDelay(200, TimeUnit.MILLISECONDS));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectNext(conversion)
                .expectComplete()
                .verify();

        for (int i = 0; i < 2; i++) {
            RecordedRequest mockRequest = remoteMockServer.takeRequest();
            assertThat(mockRequest.getMethod()).isEqualTo("GET");
            assertThat(mockRequest.getRequestLine()).contains(
                    request.getConvertCurrency().name() + request.getBaseCurrency().name(),
                    token);
        }

    }

    @Test
    void whenServerIsUnavailable2Times_AndAnswerAfterOneRetry_CheckRetry_MaxRetryAttemptsExceed() throws InterruptedException {
        final String responseStr = " {\"status\":\"200\",\"message\":\"rates\",\"data\":{\"USDRUB\":\"64.1824\"}}";

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(responseStr));

        final ServiceRequest request = TestServiceRequestBuilder.aRequest().build();
        Mono<Conversion> responseMono = currencyService.getRateMono(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(
                        throwable -> throwable.getClass().toString().contains("RetryExhaustedException") &&
                                throwable.getMessage().contains("Retries exhausted")
                ).verify();


        for (int i = 0; i < 2; i++) {
            RecordedRequest mockRequest = remoteMockServer.takeRequest();
            assertThat(mockRequest.getMethod()).isEqualTo("GET");
            assertThat(mockRequest.getRequestLine()).contains(
                    request.getConvertCurrency().name() + request.getBaseCurrency().name(),
                    token);
        }
    }

}