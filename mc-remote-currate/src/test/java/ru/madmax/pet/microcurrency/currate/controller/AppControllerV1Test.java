package ru.madmax.pet.microcurrency.currate.controller;

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
import ru.madmax.pet.microcurrency.currate.exception.IllegalModelStructureException;
import ru.madmax.pet.microcurrency.currate.model.TestCurrencyRequestBuilder;
import ru.madmax.pet.microcurrency.currate.model.TestResponseBuilder;
import ru.madmax.pet.microcurrency.currate.service.CurrencyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_GUID_KEY;

@ActiveProfiles("test")
@WebFluxTest(controllers = {AppControllerV1.class, ExceptionHandlerController.class})                   //аннотация автоматом конфигурит webTestClient
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AppControllerV1Test {
    final WebTestClient webTestClient;
    final ObjectMapper objectMapper;
    @MockBean
    private CurrencyService currencyService;

    @Test
    void currencyCorrectRequest_AndCheckAnswerAndHeader() throws Exception {
        var response = TestResponseBuilder.aResponse().build();
        String responseString = objectMapper.writeValueAsString(response);
        when(currencyService.getRateMono(any())).thenReturn(Mono.just(response));
        String stringContent = objectMapper.writeValueAsString(TestCurrencyRequestBuilder.aRequest().build());

        var receivedContent = webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        assertThat(receivedContent).isEqualTo(responseString);
    }

    @Test
    void currencyCorrectRequest_WithFloatAmount_WithPointSeparation_AndCheckAnswerAndHeader() throws Exception {
        var response = TestResponseBuilder.aResponse().build();
        String responseString = objectMapper.writeValueAsString(response);
        when(currencyService.getRateMono(any())).thenReturn(Mono.just(response));
        String stringContent = "{\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":\"123.32\"}";

        var receivedContent = webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        assertThat(receivedContent).isEqualTo(responseString);
    }

    @Test
    void currencyCorrectRequest_WithFloatAmount_WithCommaSeparation_AndCheckAnswerAndHeader() throws Exception {
        var response = TestResponseBuilder.aResponse().build();
        String responseString = objectMapper.writeValueAsString(response);
        when(currencyService.getRateMono(any())).thenReturn(Mono.just(response));
        String stringContent = "{\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":\"123,32\"}";

        var receivedContent = webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("Cannot deserialize value of type"))
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("123,32"));

    }

    @Test
    void currencyRequest_withoutBaseCurrency_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"convert_currency\":\"USD\",\"base_amount\":50000}";
        webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Base currency is wrong or not defined");

    }

    @Test
    void currencyRequest_withWrongBaseCurrency_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"base_currency\":\"ZZZ\",\"convert_currency\":\"USD\",\"base_amount\":50000}";
        webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("Cannot deserialize value of type"))
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("ZZZ"));

    }

    @Test
    void currencyRequest_withoutConversionCurrency_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"base_currency\":\"RUB\",\"base_amount\":50000}";
        webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Currency for conversion is wrong or not defined");

    }

    @Test
    void currencyRequest_withWrongConversionCurrency_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"base_currency\":\"RUB\",\"convert_currency\":\"XXX\",\"base_amount\":50000}";
        webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("Cannot deserialize value of type"))
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("XXX"));


    }

    @Test
    void currencyRequest_withoutAmount_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"base_currency\":\"RUB\",\"convert_currency\":\"USD\"}";
        webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Amount is not defined");
    }

    @Test
    void currencyRequest_withNegativeAmount_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":-50000}";
        webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Amount must be more than 0");

    }

    @Test
    void currencyRequest_withZeroAmount_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":0}";
        webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Amount must be more than 0");

    }

    @Test
    void currencyRequest_withNonNumericAmount_AndGet400Status_WithDetailsHeaders() {
        String stringContent = "{\"base_currency\":\"RUB\",\"convert_currency\":\"USD\",\"base_amount\":\"5000a\"}";
        webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("Cannot deserialize value of type"))
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("5000a"));
    }

    @Test
    void currencyRequest_AndInternalExceptionOccurres_AndGet500Status_WithDetailsHeaders() throws JsonProcessingException {
        Throwable error = new IllegalModelStructureException("test exception", "presentation");
        when(currencyService.getRateMono(any())).thenThrow(error);
        String stringContent = objectMapper.writeValueAsString(TestCurrencyRequestBuilder.aRequest().build());

        var receivedContent = webTestClient
                .post()
                .uri("/api/v1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("test exception"))
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("IllegalModelStructureException"));

    }

}