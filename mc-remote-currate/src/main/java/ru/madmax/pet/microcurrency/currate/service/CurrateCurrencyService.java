package ru.madmax.pet.microcurrency.currate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import ru.madmax.pet.microcurrency.currate.exception.IllegalAmountException;
import ru.madmax.pet.microcurrency.currate.exception.IllegalModelStructureException;
import ru.madmax.pet.microcurrency.currate.exception.IllegalRateException;
import ru.madmax.pet.microweather.common.model.ConversionRequest;
import ru.madmax.pet.microweather.common.model.ConversionResponse;
import ru.madmax.pet.microcurrency.currate.model.ConversionResponseX;

import java.time.Duration;

public class CurrateCurrencyService implements CurrencyService {
    private final WebClient webClient;
    private final ConversionService conversionService;

    private final ObjectMapper objectMapper;
    private final Integer requestRetryDuration;
    private final Integer requestRetryAttempts;
    private final String remotePath;
    private final String remoteAccessKey;
    private final String remoteHost;

    public CurrateCurrencyService(HttpClient httpClient,
                                  ConversionService conversionService,
                                  ObjectMapper objectMapper,
                                  @Value("${app.key}") String remoteAccessKey,
                                  @Value("${app.url}") String remoteHost,
                                  @Value("${app.path}") String remotePath,
                                  @Value("${app.request.retry.duration}") Integer requestRetryDuration,
                                  @Value("${app.request.retry.attempts}") Integer requestRetryAttempts) {
        this.webClient = WebClient.builder()
                .baseUrl(remoteHost)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.requestRetryDuration = requestRetryDuration;
        this.requestRetryAttempts = requestRetryAttempts;
        this.remotePath = remotePath;
        this.remoteHost = remoteHost;
        this.objectMapper = objectMapper;
        this.remoteAccessKey = remoteAccessKey;
        this.conversionService = conversionService;
    }


    @Override
    public Mono<ConversionResponse> getRateMono(ConversionRequest request) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(remotePath)
                        .queryParam("get", "rates")
                        .queryParam("pair", getCurrencyPairParamFromConversionRequest(request))
                        .queryParam("key", remoteAccessKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(str -> {
                    try {
                        var response = (ConversionResponse)objectMapper.readValue(str, ConversionResponseX.class);
                        response.setSource(remoteHost);
                        response.setAmount(conversionService.covert(
                                request.getAmount(), response.getRate()
                        ));
                        return Mono.just(response);
                    } catch (JsonProcessingException | IllegalRateException | IllegalAmountException e) {
                        throw new IllegalModelStructureException(e.getMessage(), str);
                    }
                })
                .retryWhen(Retry.backoff(requestRetryAttempts, Duration.ofMillis(requestRetryDuration))
                        .filter(throwable -> !(throwable instanceof IllegalModelStructureException)));
    }

    private String getCurrencyPairParamFromConversionRequest(ConversionRequest request) {
        return request.getConvert().name() + request.getBase().name();
    }

}
