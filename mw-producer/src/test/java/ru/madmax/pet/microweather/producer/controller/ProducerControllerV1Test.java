package ru.madmax.pet.microweather.producer.controller;

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
import ru.madmax.pet.microweather.producer.exception.WrongSourceException;
import ru.madmax.pet.microweather.producer.model.RequestDTO;
import ru.madmax.pet.microweather.producer.model.TestRequestDTOBuilder;
import ru.madmax.pet.microweather.producer.service.WeatherService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;


@ActiveProfiles("test")
@WebFluxTest(controllers = {ProducerControllerV1.class, ExceptionHandlerController.class})  //для webTestClient
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ProducerControllerV1Test {
    final WebTestClient webTestClient;
    @MockBean
    WeatherService weatherFacadeService;

    @Test
    void weatherCorrectRequest_AndCheckAnswerAndHeader() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        var request = TestRequestDTOBuilder.aRequestDTO().build();
        String requestStr = objectMapper.writeValueAsString(request);
        when(weatherFacadeService.registerRequest(any(RequestDTO.class))).thenReturn(Mono.just("test-guid"));

        var receivedContent = webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();
        assertThat(receivedContent).isEqualTo("test-guid");
    }

    @Test
    void weatherRequest_WithoutLatitude_AndGet400Status_WithDetailsHeader() {
        String requestStr = "{\"source\":\"first\",\"point\":{\"lon\":46.001373}}";
        when(weatherFacadeService.registerRequest(any(RequestDTO.class))).thenReturn(Mono.just("test-guid"));

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Latitude is not set");
    }

    @Test
    void weatherRequest_WithoutLongitude_AndGet400Status_WithDetailsHeader(){
        String requestStr = "{\"source\":\"first\",\"point\":{\"lat\":51.534986}}";
        when(weatherFacadeService.registerRequest(any(RequestDTO.class))).thenReturn(Mono.just("test-guid"));

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Longitude is not set");
    }

    @Test
    void weatherRequest_WithoutPoint_AndGet400Status_WithDetailsHeader() {
        String requestStr = "{\"source\":\"first\"}";
        when(weatherFacadeService.registerRequest(any(RequestDTO.class))).thenReturn(Mono.just("test-guid"));

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Point is not set");
    }

    @Test
    void weatherRequest_WithoutSource_AndGet400Status_WithDetailsHeader() {
        String requestStr = "{\"point\":{\"lat\":51.534986,\"lon\":46.001373}}";
        when(weatherFacadeService.registerRequest(any(RequestDTO.class))).thenReturn(Mono.just("test-guid"));

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Source is not set");
    }

    @Test
    void weatherRequest_WithWrongSource_AndGet400Status_WithDetailsHeader() {
        String requestStr = "{\"source\":\"fast\",\"point\":{\"lat\":51.534986,\"lon\":46.001373}}";
        Throwable wrongSourceError = new WrongSourceException("fast");
        doThrow(wrongSourceError).when(weatherFacadeService).registerRequest(any(RequestDTO.class));

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Wrong source");
    }

    @Test
    void weatherRequest_WhenLatitudeIsNotNumberValue_AndGet400Status_WithDetailsHeaders() {
        String requestStr = "{\"source\":\"first\",\"point\":{\"lat\":5A1,\"lon\":46.001373}}";
        Throwable wrongSourceError = new WrongSourceException("fast");
        doThrow(wrongSourceError).when(weatherFacadeService).registerRequest(any(RequestDTO.class));

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().exists(HEADER_REQUEST_ERROR_KEY);
    }

    @Test
    void weatherRequest_AndThrowsAppException_AndGet500Status_WithDetailsHeaders() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        var request = TestRequestDTOBuilder.aRequestDTO().build();
        String requestStr = objectMapper.writeValueAsString(request);
        Throwable error = new RuntimeException("Test error");
        doThrow(error).when(weatherFacadeService).registerRequest(any(RequestDTO.class));

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Test error");
    }

    @Test
    void weatherRequest_andThrowsErrorInMono_AndGet500Status_andCheckHeaders() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        var request = TestRequestDTOBuilder.aRequestDTO().build();
        String requestStr = objectMapper.writeValueAsString(request);
        Throwable error = new RuntimeException("Test error");
        when(weatherFacadeService.registerRequest(any(RequestDTO.class))).thenReturn(Mono.error(error));

        webTestClient
                .post()
                .uri("/api/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Test error");
    }
}