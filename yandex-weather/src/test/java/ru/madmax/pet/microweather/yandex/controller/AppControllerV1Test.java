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
import ru.madmax.pet.microweather.yandex.exception.AppYandexException;
import ru.madmax.pet.microweather.yandex.model.Point;
import ru.madmax.pet.microweather.yandex.model.PointBuilder;
import ru.madmax.pet.microweather.yandex.model.WeatherBuilder;
import ru.madmax.pet.microweather.yandex.service.WeatherLoaderService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ActiveProfiles("test")
@WebFluxTest(controllers = {AppControllerV1.class, ExceptionHandlerController.class})                   //аннотация автоматом конфигурит webTestClient
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AppControllerV1Test {
    private final WebTestClient webTestClient;
    @MockBean
    private WeatherLoaderService loaderService;

    @Test
    void weatherCorrectRequest() throws Exception {

        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.just(WeatherBuilder.aWeather().build()));
        String stringContent = new ObjectMapper().writeValueAsString(PointBuilder.aPoint().build());

        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header("request-guid", "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("request-guid", "testguid")
/*
                .expectBody(Greeting.class).value(greeting -> {
                    assertThat(greeting.getMessage()).isEqualTo("Hello, Spring!");
                });
*/
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();
    }

    @Test
    void weatherRequest_withoutLatitude_AndGet400Status() {
        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.just(WeatherBuilder.aWeather().build()));
        String stringContent = "{\"lon\":46.001373}";
        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header("guid", "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void weatherRequest_withoutLongitude_AndGet400Status()  {
        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.just(WeatherBuilder.aWeather().build()));
        String stringContent = "{\"lat\":51.534986}";
        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header("guid", "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void weatherRequest_withoutGUIDHeader_AndGet400Status() throws Exception {
        when(loaderService.requestWeatherByPoint(any(Point.class))).thenReturn(Mono.just(WeatherBuilder.aWeather().build()));
        String stringContent = new ObjectMapper().writeValueAsString(PointBuilder.aPoint().build());
        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
//                .header("guid", "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void weatherRequest_AppExceptionInService_AndGet400Status() throws JsonProcessingException {
        AppYandexException error = new AppYandexException("test error");
        doThrow(error).when(loaderService).requestWeatherByPoint(any(Point.class));
        String stringContent = new ObjectMapper().writeValueAsString(PointBuilder.aPoint().build());

        webTestClient
                .post()
                .uri("/api/v1/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringContent))
                .header("request-guid", "testguid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();

    }
}