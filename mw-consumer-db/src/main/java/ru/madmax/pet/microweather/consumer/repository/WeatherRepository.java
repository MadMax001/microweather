package ru.madmax.pet.microweather.consumer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;

@Repository
public interface WeatherRepository extends ReactiveCrudRepository<WeatherDomain, String> {

    @Override
    @Query("SELECT * FROM weather WHERE id = :id")
    Mono<WeatherDomain> findById(String id);
}
