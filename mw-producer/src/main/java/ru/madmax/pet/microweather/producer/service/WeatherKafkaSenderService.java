package ru.madmax.pet.microweather.producer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.madmax.pet.microweather.producer.model.Weather;

import static java.util.Objects.isNull;

@Service
public class WeatherKafkaSenderService implements WeatherProducerService {

    private final String sendClientTopic;
    private final Integer replicationFactor;
    private final Integer partitionNumber;
    private final LogService logService;

    private final KafkaTemplate<String , Weather> kafkaTemplate;

    public WeatherKafkaSenderService(@Value("${spring.kafka.topic.name}") String sendClientTopic,
                                     @Value("${spring.kafka.replication.factor}") Integer replicationFactor,
                                     @Value("${spring.kafka.partition.number}") Integer partitionNumber,
                                     KafkaTemplate<String, Weather> kafkaTemplate,
                                     LogService logService) {
        this.sendClientTopic = sendClientTopic;
        this.kafkaTemplate = kafkaTemplate;
        this.replicationFactor = replicationFactor;
        this.partitionNumber = partitionNumber;
        this.logService = logService;
    }

    @Async
    @Override
    public void produceWeather(String key, Weather weather) {
        var sendResult = kafkaTemplate.send(sendClientTopic, key, weather);
        sendResult.whenComplete((result, ex) -> {
            if (isNull(ex)) {
                logService.info(String.format("Successful sending[%s]: %s",
                                                key,
                                                result));
            } else {
                logService.error(String.format("Error on sending[%s]: %s:%s",
                                                key,
                                                ex.getClass().getName(),
                                                ex.getMessage()));
            }
        });
    }
}
