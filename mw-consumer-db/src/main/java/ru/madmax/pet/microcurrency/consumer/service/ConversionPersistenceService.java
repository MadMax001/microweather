package ru.madmax.pet.microcurrency.consumer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.consumer.model.ConversionEntity;
import ru.madmax.pet.microcurrency.consumer.repository.ConversionRepository;

@Service
@RequiredArgsConstructor
public class ConversionPersistenceService implements ConversionPersistence {
    private final ConversionRepository repository;
    @Override
    public Mono<ConversionEntity> save(ConversionEntity weather) {
        return repository.save(weather);
    }

}
