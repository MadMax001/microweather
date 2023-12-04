package ru.madmax.pet.microweather.producer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.madmax.pet.microweather.producer.exception.AppProducerException;
import ru.madmax.pet.microweather.producer.model.MessageDTO;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class WeatherKafkaSenderService implements WeatherProducerService {

    private final String sendClientTopic;
    private final LogService logService;

    private final KafkaTemplate<String , MessageDTO> kafkaTemplate;

    public WeatherKafkaSenderService(@Value("${spring.kafka.topic.name}") String sendClientTopic,
                                     KafkaTemplate<String, MessageDTO> kafkaTemplate,
                                     LogService logService) {
        this.sendClientTopic = sendClientTopic;
        this.kafkaTemplate = kafkaTemplate;
        this.logService = logService;
    }

    @Override
    public void produceMessage(String key, MessageDTO message) {
        try {
            var sendResult = kafkaTemplate.send(sendClientTopic, key, message);

            sendResult.whenComplete((result, ex) -> {
                if (isNull(ex)) {
                    logService.info(String.format("Successful sending[%s]: %s",
                            key,
                            result));
                } else {
                    logService.error(String.format("Error on sending[%s]: %s: %s%nCause: %s: %s",
                            key,
                            ex.getClass().getName(),
                            ex.getMessage(),
                            (nonNull(ex.getCause())? ex.getCause().getClass().getName(): ""),
                            (nonNull(ex.getCause())? ex.getMessage(): "")
                            ));

                }
            });

        } catch (KafkaException ke) {

            throw new AppProducerException(ke.getCause());
        }

    }

}
