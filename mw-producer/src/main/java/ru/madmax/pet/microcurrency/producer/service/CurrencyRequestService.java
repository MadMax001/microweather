package ru.madmax.pet.microcurrency.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.producer.model.RequestParams;
import ru.madmax.pet.microcurrency.common.model.Conversion;
import ru.madmax.pet.microcurrency.common.model.ServiceRequest;

public interface CurrencyRequestService {
    Mono<Conversion> sendRequest(ServiceRequest request, RequestParams params);
}
