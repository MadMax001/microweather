package ru.madmax.pet.microweather.consumer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;
import ru.madmax.pet.microweather.consumer.repository.WeatherRepository;

@Service
@RequiredArgsConstructor
public class WeatherPersistenceService implements WeatherPersistence{
    private final WeatherRepository repository;
    @Override
    public Mono<WeatherDomain> save(WeatherDomain weather) {
        return repository.save(weather);
    }

}
