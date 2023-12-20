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
    private final String yaWeatherPath;

    public ForecastWeatherLoaderService(HttpClient httpClient,
                                        @Value("${app.weather.key}") String yaWeatherAPIKey,
                                        @Value("${app.weather.url}") String yaWeatherURL,
                                        @Value("${app.weather.path}") String yaWeatherPath,
                                        @Value("${app.weather.retry.duration}") Integer yaWeatherRetryDuration,
                                        @Value("${app.weather.retry.attempts}") Integer yaWeatherRetryAttempts) {
        this.webClient = WebClient.builder()
                .baseUrl(yaWeatherURL)
                .defaultHeader("X-Yandex-API-Key", yaWeatherAPIKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.yaWeatherRetryDuration = yaWeatherRetryDuration;
        this.yaWeatherRetryAttempts = yaWeatherRetryAttempts;
        this.yaWeatherPath = yaWeatherPath;
    }


    @Override
    public Mono<Weather> requestWeatherByPoint(Point point) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(yaWeatherPath)
                        .queryParam("lat", point.getLat().toString())
                        .queryParam("lon", point.getLon().toString())
                        .build())
                .retrieve()
                .bodyToMono(Weather.class)
                .retryWhen(Retry.backoff(yaWeatherRetryAttempts, Duration.ofMillis(yaWeatherRetryDuration)));
    }
}
