package ru.madmax.pet.microcurrency.currate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import ru.madmax.pet.microcurrency.currate.exception.IllegalAmountException;
import ru.madmax.pet.microcurrency.currate.exception.IllegalModelStructureException;
import ru.madmax.pet.microcurrency.currate.exception.IllegalRateException;
import ru.madmax.pet.microcurrency.currate.model.RemoteResponse;
import ru.madmax.pet.microcurrency.common.model.ServiceRequest;
import ru.madmax.pet.microcurrency.common.model.Conversion;

import java.time.Duration;

@Service
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
    public Mono<Conversion> getRateMono(final ServiceRequest request) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(remotePath)
                        .queryParam("get", "rates")
                        .queryParam("pair", getCurrencyPairParamFromRequest(request))
                        .queryParam("key", remoteAccessKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(str -> Mono.just(createConversionFromResponse(str, request)))
                .retryWhen(Retry.backoff(requestRetryAttempts, Duration.ofMillis(requestRetryDuration))
                        .filter(throwable -> !(throwable instanceof IllegalModelStructureException)));
    }

    private Conversion createConversionFromResponse(String str, ServiceRequest request) {
        try {
            var remoteResponse = objectMapper.readValue(str, RemoteResponse.class);
            var conversion = new Conversion();
            conversion.setBase(request.getBaseCurrency());
            conversion.setConvert(request.getConvertCurrency());
            conversion.setBaseAmount(request.getBaseAmount());
            conversion.setConversionAmount(conversionService.covert(
                    request.getBaseAmount(), remoteResponse.getRate())
            );
            conversion.setSource(remoteHost);
            return conversion;
        } catch (JsonProcessingException | IllegalRateException | IllegalAmountException e) {
            throw new IllegalModelStructureException(e.getMessage(), str);
        }
    }

    private String getCurrencyPairParamFromRequest(ServiceRequest request) {
        return request.getConvertCurrency().name() + request.getBaseCurrency().name();
    }

}
