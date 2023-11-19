package ru.madmax.pet.kafkatest.weather.yandex.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import ru.madmax.pet.kafkatest.weather.yandex.model.Point;
import ru.madmax.pet.kafkatest.weather.yandex.model.Weather;
import ru.madmax.pet.kafkatest.weather.yandex.service.WeatherLoaderService;

import javax.validation.Valid;
import java.util.stream.Collectors;

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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<Throwable> handleValidationExceptions(WebExchangeBindException ex) {
        ex.printStackTrace();
        log.error(ex.getBindingResult().getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", ")));
        return Mono.error(ex);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<Throwable> handleValidationExceptions1(ServerWebInputException ex) {
        log.error(ex.getMessage());
        return Mono.error(ex);
    }




}
