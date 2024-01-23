package ru.madmax.pet.microweather.yandex.controller;

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
import ru.madmax.pet.microweather.common.model.*;
import ru.madmax.pet.microweather.yandex.exception.AppYandexException;
import ru.madmax.pet.microweather.yandex.service.WeatherLoaderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_GUID_KEY;

@ActiveProfiles("test")
@WebFluxTest(controllers = {AppControllerV1.class, ExceptionHandlerController.class})                   //аннотация автоматом конфигурит webTestClient
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AppControllerV1Test {
    final WebTestClient webTestClient;
    @MockBean
    private WeatherLoaderService loaderService;

    @Test
    void weatherCorrectRequest_AndCheckAnswerAndHeader() throws Exception {
        var objectMapper = new ObjectMapper();
        var weather = TestWeatherBuilder.aWeather().build();
        String weatherStr = objectMapper.writeValueAsString(weather);
        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.just(weather));
        String stringContent = objectMapper.writeValueAsString(TestPointBuilder.aPoint().build());

        var receivedContent = webTestClient
                .post()
                .uri("/api/v1/weather")
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

        assertThat(receivedContent).isEqualTo(weatherStr);
    }

    @Test
    void weatherRequest_withoutLatitude_AndGet400Status_WithDetailsHeaders() {
        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.just(TestWeatherBuilder.aWeather().build()));
        String stringContent = "{\"lon\":46.001373}";
        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Latitude is not set");

    }

    @Test
    void weatherRequest_withoutLongitude_AndGet400Status_WithDetailsHeaders() {
        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.just(TestWeatherBuilder.aWeather().build()));
        String stringContent = "{\"lat\":51.534986}";
        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Longitude is not set");

    }

    @Test
    void weatherRequest_WithoutGUIDHeader_AndGet400Status_WithDetailsHeaders() throws Exception {
        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.just(TestWeatherBuilder.aWeather().build()));
        String stringContent = new ObjectMapper().writeValueAsString(TestPointBuilder.aPoint().build());
        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
//                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HEADER_REQUEST_GUID_KEY)
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY, containsString("Required header '" + HEADER_REQUEST_GUID_KEY + "' is not present"));

    }

    @Test
    void weatherRequest_AppExceptionInService_AndGet500Status_WithDetailsHeaders() throws JsonProcessingException {
        AppYandexException error = new AppYandexException("test error");
        doThrow(error).when(loaderService).requestWeatherByPoint(any(Point.class));
        String stringContent = new ObjectMapper().writeValueAsString(TestPointBuilder.aPoint().build());

        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "test error");


    }

    @Test
    void weatherRequest_andThrowsErrorInMono_AndGet500Status_andCheckHeaders() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        Throwable error = new AppYandexException("Error in process");
        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.error(error));

        String stringContent = objectMapper.writeValueAsString(TestPointBuilder.aPoint().build());
        var receivedContent = webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Error in process")
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        assertThat(receivedContent).isNull();

    }

    @Test
    void weatherRequest_whenLatitudeIsNotNumberValue_AndGet400Status() {
        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.just(TestWeatherBuilder.aWeather().build()));
        String stringContent = "{\"lon\":46.001373, \"lat\":\"51A\"}";
        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header(HEADER_REQUEST_GUID_KEY, "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals(HEADER_REQUEST_GUID_KEY, "testguid")
                .expectHeader().value(HEADER_REQUEST_ERROR_KEY,
                        containsString("Cannot deserialize value of type `java.lang.Double` from String"));

    }
}