package ru.madmax.pet.microcurrency.currate.service;

import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.common.model.ServiceRequest;
import ru.madmax.pet.microweather.common.model.Conversion;

public interface CurrencyService {
    Mono<Conversion> getRateMono(ServiceRequest request);
}
