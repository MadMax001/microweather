package ru.madmax.pet.microweather.weather.yandex.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import ru.madmax.pet.microweather.weather.yandex.model.Point;
import ru.madmax.pet.microweather.weather.yandex.model.Weather;

import java.time.Duration;
import java.util.concurrent.TimeoutException;


@Service
public class ForecastWeatherLoaderService implements WeatherLoaderService {
    private final WebClient webClient;
    private final Integer yaWeatherRetryDuration;

    public ForecastWeatherLoaderService(HttpClient httpClient,
                                        @Value("${app.weather.key}") String yaWeatherAPIKey,
                                        @Value("${app.weather.url}") String yaWeatherURL,
                                        @Value("${app.weather.retry-duration}") Integer yaWeatherRetryDuration) {
        this.webClient = WebClient.builder()
                .baseUrl(yaWeatherURL)
                .defaultHeader("X-Yandex-API-Key", yaWeatherAPIKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.yaWeatherRetryDuration = yaWeatherRetryDuration;
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
                .bodyToMono(Weather.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(yaWeatherRetryDuration))
                        .filter(error -> error instanceof HttpServerErrorException ||
                                        error instanceof WebClientRequestException &&
                                        error.getCause() instanceof TimeoutException));
    }
}
