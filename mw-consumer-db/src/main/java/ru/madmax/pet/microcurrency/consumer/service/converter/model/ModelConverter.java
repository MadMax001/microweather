package ru.madmax.pet.microcurrency.consumer.service.converter.model;

public interface ModelConverter<K, M, D> {
    D convert (K key, M model);
}
