package ru.madmax.pet.microcurrency.producer.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.producer.model.CurrencyRequestX;

public interface CurrencyService {
    Mono<String> registerRequest(CurrencyRequestX request);
}
