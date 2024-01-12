package ru.madmax.pet.microweather.consumer.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import ru.madmax.pet.microweather.consumer.model.ErrorDomain;

@Repository
public interface ErrorRepository extends ReactiveCrudRepository<ErrorDomain, String> {
}
