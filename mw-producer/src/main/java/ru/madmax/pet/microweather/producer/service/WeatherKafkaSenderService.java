package ru.madmax.pet.microweather.producer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;
import ru.madmax.pet.microweather.producer.model.Weather;

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
        var sendResult = kafkaTemplate.send(sendClientTopic, weather);
        sendResult.addCallback(
                new SuccessCallback<SendResult<String, Weather>>() {
                    @Override
                    public void onSuccess(SendResult<String, Weather> result) {
                        logService.info(String.format("Successful sending[%s]: %s", key, result));
                    }
                },
                new FailureCallback() {
                    @Override
                    public void onFailure(Throwable ex) {
                        logService.error(String.format("Error on sending[%s]: %s:%s", key, ex.getClass().getName(), ex.getMessage()));
                    }
                });
    }
}
