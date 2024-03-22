package ru.madmax.pet.microcurrency.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.producer.model.RemoteConversionResponseX;
import ru.madmax.pet.microcurrency.producer.model.RequestParams;
import ru.madmax.pet.microweather.common.model.ServiceRequest;

public interface CurrencyRequestService {
    Mono<RemoteConversionResponseX> sendRequest(ServiceRequest request, RequestParams params);
}
