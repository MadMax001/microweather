package ru.madmax.pet.microweather.yandex.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.yandex.model.Point;
import ru.madmax.pet.microweather.yandex.model.Weather;
import ru.madmax.pet.microweather.yandex.service.WeatherLoaderService;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class AppControllerV1 {
    private final WeatherLoaderService loaderService;

    @PostMapping("/weather")
    public ResponseEntity<Mono<Weather>> weatherRequest(@RequestBody @Valid Point point,
                                                       @RequestHeader(name="request-guid") String requestGuid) {

        var monoWeather = loaderService.requestWeatherByPoint(
                Point.builder()
                        .lat(point.getLat())
                        .lon(point.getLon())
                        .build()
        );
        return ResponseEntity.ok()
                .header("request-guid", requestGuid)
                .body(monoWeather);
    }


}
