package ru.madmax.pet.microweather.consumer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.consumer.model.ErrorDomain;

@Repository
public interface ErrorRepository extends ReactiveCrudRepository<ErrorDomain, String> {
    @Override
    @Query("SELECT * FROM error WHERE id = :id")
    Mono<ErrorDomain> findById(String id);

}
