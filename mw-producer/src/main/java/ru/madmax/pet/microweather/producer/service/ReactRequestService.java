package ru.madmax.pet.microweather.producer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import ru.madmax.pet.microweather.producer.model.Point;
import ru.madmax.pet.microweather.producer.model.RequestParams;
import ru.madmax.pet.microweather.producer.model.Weather;

import java.time.Duration;

@Service
public class ReactRequestService implements WeatherRequestService {
    private final HttpClient httpClient;
    private final Integer weatherRetryDuration;
    private final Integer weatherRetryAttempts;

    public ReactRequestService(HttpClient httpClient,
                               @Value("${app.weather.retry.duration}") Integer weatherRetryDuration,
                               @Value("${app.weather.retry.attempts}") Integer weatherRetryAttempts) {
        this.weatherRetryDuration = weatherRetryDuration;
        this.weatherRetryAttempts = weatherRetryAttempts;
        this.httpClient = httpClient;
    }

    @Override
    public Mono<Weather> registerRequest(Point point, RequestParams params) {
        var webClient = WebClient.builder()
                .baseUrl(String.format("%s://%s",
                        params.getUrl().getProtocol(),
                        params.getUrl().getAuthority()))
                .defaultHeader("request-guid", params.getGuid())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(params.getUrl().getPath())
                        .build())
                .body(BodyInserters.fromValue(point))
                .retrieve()
                .bodyToMono(Weather.class)
                .retryWhen(Retry.backoff(weatherRetryAttempts, Duration.ofMillis(weatherRetryDuration)));
    }
}
