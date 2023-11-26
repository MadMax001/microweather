package ru.madmax.pet.microweather.producer.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfiguration {                                   //todo надо ли?
    private final ProducerFactory<Object, Object> producerFactory;
}
