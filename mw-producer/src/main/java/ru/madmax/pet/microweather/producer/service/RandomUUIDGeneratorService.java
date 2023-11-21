package ru.madmax.pet.microweather.producer.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RandomUUIDGeneratorService implements UUIDGeneratorService {

    @Override
    public String randomGenerate() {
        return UUID.randomUUID().toString();
    }
}
