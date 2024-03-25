package ru.madmax.pet.microcurrency.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.producer.model.ClientRequest;

public interface CurrencyService {
    Mono<String> registerRequest(ClientRequest request);
}
