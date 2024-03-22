package ru.madmax.pet.microcurrency.producer.service;

import reactor.core.publisher.Mono;

public interface CurrencyService {
    Mono<String> registerRequest(ClientRequestX request);
}
