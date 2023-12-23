package ru.madmax.pet.microweather.producer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import ru.madmax.pet.microweather.common.model.Point;
import ru.madmax.pet.microweather.common.model.Weather;
import ru.madmax.pet.microweather.producer.model.RequestParams;

import java.time.Duration;
import java.util.stream.Collectors;

@Service
public class ReactRequestService implements WeatherRequestService {
    private final HttpClient httpClient;
    private final LogService logService;
    private final Integer weatherRetryDuration;
    private final Integer weatherRetryAttempts;

    public ReactRequestService(HttpClient httpClient,
                               LogService logService,
                               @Value("${app.weather.retry.duration}") Integer weatherRetryDuration,
                               @Value("${app.weather.retry.attempts}") Integer weatherRetryAttempts) {
        this.weatherRetryDuration = weatherRetryDuration;
        this.weatherRetryAttempts = weatherRetryAttempts;
        this.httpClient = httpClient;
        this.logService = logService;
    }

    @Override
    public Mono<Weather> sendRequest(Point point, RequestParams params) {
        logService.info(String.format("Send [%s] to %s", params.getGuid(), params.getUrl().toString()));
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
                .exchangeToMono(response -> {
                    logResponseDetails(response, params);
                    if (response.statusCode().is5xxServerError())
                        return response.createException().flatMap(Mono::error);
                    if (response.statusCode().is2xxSuccessful())
                        return response.bodyToMono(Weather.class);
                    return Mono.empty();
                })
                .retryWhen(Retry.backoff(weatherRetryAttempts, Duration.ofMillis(weatherRetryDuration))
                        .doBeforeRetry(retry -> logService.info(String.format("Retrying, %d", retry.totalRetries()))));
    }

    private void logResponseDetails(ClientResponse response, RequestParams params) {
        var headersListString = response
                .headers()
                .asHttpHeaders()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("request-"))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
        var statusString = response.statusCode().toString();
        logService.info(String.format("Response [%s]: status: %s, specific headers {%s}",
                params.getGuid(),
                statusString,
                headersListString));
    }
}
