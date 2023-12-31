package ru.madmax.pet.microweather.yandex.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.common.model.Point;
import ru.madmax.pet.microweather.common.model.Weather;
import ru.madmax.pet.microweather.yandex.service.WeatherLoaderService;


@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class AppControllerV1 {
    private final WeatherLoaderService loaderService;

    @PostMapping("/weather")
    public Mono<ResponseEntity<Weather>> weatherRequest(@RequestBody @Valid Point point,
                                                        @RequestHeader(name="request-guid") String requestGuid) {

        var monoWeather = loaderService.requestWeatherByPoint(
                Point.builder()
                        .lat(point.getLat())
                        .lon(point.getLon())
                        .build()
        );
        return monoWeather
                .map(weather ->
                        ResponseEntity
                                .ok()
                                .header("request-guid", requestGuid)
                                .body(weather)
                )
                .onErrorResume(error->
                        Mono.just(ResponseEntity
                                .internalServerError()
                                .header("request-guid", requestGuid)
                                .header("request-error", error.getMessage())
                                .body(null))
                );
    }


}
