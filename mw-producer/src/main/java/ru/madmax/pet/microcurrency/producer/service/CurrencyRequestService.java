package ru.madmax.pet.microcurrency.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.producer.model.ConversionResponseX;
import ru.madmax.pet.microcurrency.producer.model.RequestParams;
import ru.madmax.pet.microweather.common.model.CurrencyRequest;

public interface CurrencyRequestService {
    Mono<ConversionResponseX> sendRequest(CurrencyRequest request, RequestParams params);
}
