package ru.madmax.pet.microcurrency.consumer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.consumer.model.ErrorEntity;

@Repository
public interface ErrorRepository extends ReactiveCrudRepository<ErrorEntity, String> {
    @Override
    @Query("SELECT * FROM error WHERE id = :id")
    Mono<ErrorEntity> findById(String id);

}
