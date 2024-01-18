package ru.madmax.pet.microweather.consumer.service.converter.model;

public interface ModelDomainConverter<K, M, D> {
    D convert (K key, M model);
}
