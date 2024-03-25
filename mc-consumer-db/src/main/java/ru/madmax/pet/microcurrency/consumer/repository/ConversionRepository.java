package ru.madmax.pet.microcurrency.consumer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.consumer.model.ConversionEntity;

@Repository
public interface ConversionRepository extends ReactiveCrudRepository<ConversionEntity, String> {

    @Override
    @Query("SELECT * FROM conversion WHERE id = :id")
    Mono<ConversionEntity> findById(String id);
}
