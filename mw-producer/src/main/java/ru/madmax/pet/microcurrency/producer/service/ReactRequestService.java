package ru.madmax.pet.microcurrency.producer.service;

import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import ru.madmax.pet.microcurrency.producer.exception.RemoteServiceException;
import ru.madmax.pet.microcurrency.producer.model.ConversionResponseX;
import ru.madmax.pet.microweather.common.model.CurrencyRequest;
import ru.madmax.pet.microcurrency.producer.model.RequestParams;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;
import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_GUID_KEY;

@Service
public class ReactRequestService implements CurrencyRequestService {
    private final HttpClient httpClient;
    private final LogService logService;
    private final Integer requestRetryDuration;
    private final Integer requestRetryAttempts;

    public ReactRequestService(HttpClient httpClient,
                               LogService logService,
                               @Value("${app.request.retry.duration}") Integer requestRetryDuration,
                               @Value("${app.request.retry.attempts}") Integer requestRetryAttempts) {
        this.requestRetryDuration = requestRetryDuration;
        this.requestRetryAttempts = requestRetryAttempts;
        this.httpClient = httpClient;
        this.logService = logService;
    }

    @Override
    public Mono<ConversionResponseX> sendRequest(CurrencyRequest request, RequestParams params) {
        logService.info(
                params.getGuid(),
                String.format("Send to %s", params.getUrl().toString()));

        var webClient = WebClient.builder()
                .baseUrl(String.format("%s://%s",
                        params.getUrl().getProtocol(),
                        params.getUrl().getAuthority()))
                .defaultHeader(HEADER_REQUEST_GUID_KEY, params.getGuid())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(params.getUrl().getPath())
                        .build())
                .body(BodyInserters.fromValue(request))
                .exchangeToMono(response -> {
                    logResponseDetails(response, params);
                    if (response.statusCode().is2xxSuccessful())
                        return createSuccessMonoResponse(response);
                    return createErrorMonoResponse(response);
                })
                .retryWhen(Retry.backoff(requestRetryAttempts, Duration.ofMillis(requestRetryDuration))
                        .doBeforeRetry(retry -> logService.info(
                                params.getGuid(),
                                String.format("Retrying, %d", retry.totalRetries())))
                        .filter(throwable -> {
                                logErrorDetails(params.getGuid(), throwable);
                                return checkForRetryByError(throwable);
                        }));
    }

    private void logResponseDetails(ClientResponse response, RequestParams params) {
        var headersListString = response
                .headers()
                .asHttpHeaders()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("X-request-"))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
        var statusString = response.statusCode().toString();
        logService.info(
                params.getGuid(),
                String.format("Response status: %s, specific headers {%s}",
                    statusString,
                    headersListString)
        );
    }

    private Mono<ConversionResponseX> createSuccessMonoResponse(ClientResponse response) {
        return response.bodyToMono(ConversionResponseX.class);
    }

    private Mono<ConversionResponseX> createErrorMonoResponse(final ClientResponse response) {
        List<String> requestErrorHeaderValues =
                (response.headers().asHttpHeaders().get(HEADER_REQUEST_ERROR_KEY));
        if (requestErrorHeaderValues != null && !requestErrorHeaderValues.isEmpty()) {
            Throwable error = new RemoteServiceException(requestErrorHeaderValues.get(0));
            return Mono.error(error);
        } else
            return response.createException().flatMap(Mono::error);
    }


    private void logErrorDetails(String guid, Throwable throwable) {
        logService.error(
                guid,
                String.format("Error in response details: %s: %s",
                throwable.getClass().getName(), throwable.getMessage())
        );
    }

    private boolean checkForRetryByError(Throwable throwable) {
        return throwable instanceof ReadTimeoutException ||
                throwable instanceof WebClientResponseException &&
                        (
                                throwable.getMessage().startsWith("502") ||
                                        throwable.getMessage().startsWith("503")
                        ) ||
                throwable instanceof WebClientRequestException &&
                        throwable.getCause() instanceof TimeoutException;
    }

}
