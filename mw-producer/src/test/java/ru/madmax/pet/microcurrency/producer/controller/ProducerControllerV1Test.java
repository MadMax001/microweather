package ru.madmax.pet.microcurrency.producer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.producer.exception.RemoteServiceException;
import ru.madmax.pet.microcurrency.producer.model.TestCurrencyRequestXBuilder;
import ru.madmax.pet.microcurrency.producer.service.CurrencyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_GUID_KEY;

//{"source":"www.site1.ru","base_currency":"RUB","convert_currency":"USD","base_amount":50000}

@ActiveProfiles("test")
@WebFluxTest(controllers = {ProducerControllerV1.class, ExceptionHandlerController.class})  //для webTestClient
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ProducerControllerV1Test {
    final WebTestClient webTestClient;
    final ObjectMapper objectMapper;
    @MockBean
    CurrencyService currencyService;

    @Test
    void currencyCorrectRequest_AndCheckAnswerAndHeader() throws JsonProcessingException {
        var request = TestCurrencyRequestXBuilder.aRequest().build();
        String requestStr = objectMapper.writeValueAsString(request);
        when(currencyService.registerRequest(any(ClientRequestX.class))).thenReturn(Mono.just("test-guid"));

        var receivedContent = webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().doesNotExist(HEADER_REQUEST_ERROR_KEY)
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();
        assertThat(receivedContent).isEqualTo("test-guid");
    }

    @Test
    void currencyRequest_WithoutSource_AndGet400Status_WithDetailsHeader() {
        String requestStr = "{\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":50000}";

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Source is not defined");
    }


    @Test
    void currencyCorrectRequest_WithFloatAmount_WithPointSeparation_AndCheckAnswerAndHeader() {
        String stringContent = "{\"source\":\"www.site1.ru\",\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":\"123.32\"}";
        when(currencyService.registerRequest(any(ClientRequestX.class))).thenReturn(Mono.just("test-guid"));

        var receivedContent = webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().doesNotExist(HEADER_REQUEST_ERROR_KEY)
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        assertThat(receivedContent).isEqualTo("test-guid");
    }

    @Test
    void currencyCorrectRequest_WithFloatAmount_WithCommaSeparation_AndCheckAnswerAndHeader()  {
        String stringContent = "{\"source\":\"www.site1.ru\",\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":\"123,32\"}";

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("Cannot deserialize value of type"))
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("123,32"));

    }

    @Test
    void currencyRequest_withoutBaseCurrency_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"source\":\"www.site1.ru\",\"convert_currency\":\"USD\",\"base_amount\":50000}";
        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Base currency is wrong or not defined");

    }

    @Test
    void currencyRequest_withWrongBaseCurrency_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"source\":\"www.site1.ru\",\"base_currency\":\"ZZZ\",\"convert_currency\":\"USD\",\"base_amount\":50000}";
        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("Cannot deserialize value of type"))
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("ZZZ"));

    }

    @Test
    void currencyRequest_withoutConversionCurrency_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"source\":\"www.site1.ru\",\"base_currency\":\"RUB\",\"base_amount\":50000}";
        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Currency for conversion is wrong or not defined");

    }

    @Test
    void currencyRequest_withWrongConversionCurrency_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"source\":\"www.site1.ru\",\"base_currency\":\"RUB\",\"convert_currency\":\"XXX\",\"base_amount\":50000}";
        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("Cannot deserialize value of type"))
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("XXX"));


    }

    @Test
    void currencyRequest_withoutAmount_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"source\":\"www.site1.ru\",\"base_currency\":\"RUB\",\"convert_currency\":\"USD\"}";
        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Amount is not defined");
    }

    @Test
    void currencyRequest_withNegativeAmount_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"source\":\"www.site1.ru\",\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":-50000}";
        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                //.expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Amount must be more than 0");

    }

    @Test
    void currencyRequest_withZeroAmount_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"source\":\"www.site1.ru\",\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":0}";
        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Amount must be more than 0");

    }

    @Test
    void currencyRequest_withNonNumericAmount_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"source\":\"www.site1.ru\",\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":\"5000a\"}";
        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("Cannot deserialize value of type"))
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("5000a"));
    }

    @Test
    void currencyRequest_AndInternalExceptionOccurres_AndGet500Status_WithDetailsHeaders() throws JsonProcessingException {
        Throwable error = new RemoteServiceException("test exception");
        when(currencyService.registerRequest(any(ClientRequestX.class))).thenThrow(error);
        String stringContent = objectMapper.writeValueAsString(TestCurrencyRequestXBuilder.aRequest().build());

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("test exception"));

    }


    @Test
    void weatherRequest_andThrowsErrorInMono_AndGet500Status_andCheckHeaders() throws JsonProcessingException {
        var request = TestCurrencyRequestXBuilder.aRequest().build();
        String requestStr = objectMapper.writeValueAsString(request);
        Throwable error = new RuntimeException("Test error");
        when(currencyService.registerRequest(any(ClientRequestX.class))).thenReturn(Mono.error(error));

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Test error");
    }
}