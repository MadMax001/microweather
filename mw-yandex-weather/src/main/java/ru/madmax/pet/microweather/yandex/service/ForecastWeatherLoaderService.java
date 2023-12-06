package ru.madmax.pet.microweather.yandex.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import ru.madmax.pet.microweather.common.model.Point;
import ru.madmax.pet.microweather.common.model.Weather;


import java.time.Duration;


@Service
public class ForecastWeatherLoaderService implements WeatherLoaderService {
    private final WebClient webClient;
    private final Integer yaWeatherRetryDuration;
    private final Integer yaWeatherRetryAttempts;

    public ForecastWeatherLoaderService(HttpClient httpClient,
                                        @Value("${app.weather.key}") String yaWeatherAPIKey,
                                        @Value("${app.weather.url}") String yaWeatherURL,
                                        @Value("${app.weather.retry.duration}") Integer yaWeatherRetryDuration,
                                        @Value("${app.weather.retry.attempts}") Integer yaWeatherRetryAttempts) {
        this.webClient = WebClient.builder()
                .baseUrl(yaWeatherURL)
                .defaultHeader("X-Yandex-API-Key", yaWeatherAPIKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.yaWeatherRetryDuration = yaWeatherRetryDuration;
        this.yaWeatherRetryAttempts = yaWeatherRetryAttempts;
    }


    @Override
    public Mono<Weather> requestWeatherByPoint(Point point) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/forecast")
                        .queryParam("lat", point.getLat().toString())
                        .queryParam("lon", point.getLon().toString())
                        .build())
                .retrieve()
//                .onStatus(HttpStatus::is4xxClientError,
//                        error -> Mono.error(new RuntimeException("API not found")))
//                .onStatus(HttpStatus::is5xxServerError,
//                        error -> Mono.error(new RuntimeException("Server is not responding")))
                .bodyToMono(Weather.class)
                .retryWhen(Retry.backoff(yaWeatherRetryAttempts, Duration.ofMillis(yaWeatherRetryDuration)));
    }
}
