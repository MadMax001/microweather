package ru.madmax.pet.kafkatest.weather.yandex.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import ru.madmax.pet.kafkatest.weather.yandex.model.Point;
import ru.madmax.pet.kafkatest.weather.yandex.model.Weather;
import org.springframework.http.HttpStatus;


@Service
public class ForecastWeatherLoaderService implements WeatherLoaderService {
    private final WebClient webClient;

    public ForecastWeatherLoaderService(HttpClient httpClient,
                                        @Value("${app.weather.key}") String yaWeatherAPIKey,
                                        @Value("${app.weather.url}") String yaWeatherURL) {
        this.webClient = WebClient.builder()
                .baseUrl(yaWeatherURL)
                .defaultHeader("X-Yandex-API-Key", yaWeatherAPIKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
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
                .onStatus(HttpStatus::is4xxClientError,
                        error -> Mono.error(new RuntimeException("API not found")))
                .onStatus(HttpStatus::is5xxServerError,
                        error -> Mono.error(new RuntimeException("Server is not responding")))
                .bodyToMono(Weather.class);
//                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(100)))
    }
}
