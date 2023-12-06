package ru.madmax.pet.microweather.producer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.madmax.pet.microweather.common.model.MessageDTO;
import ru.madmax.pet.microweather.producer.exception.AppProducerException;


import java.util.function.BiConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class WeatherKafkaSenderService implements WeatherProducerService {

    private final String sendClientTopic;
    private final KafkaTemplate<String , MessageDTO> kafkaTemplate;
    private final BiConsumer<String, SendResult<String, MessageDTO>> successSendingHandler;
    private final BiConsumer<String,Throwable> errorSendingHandler;

    public WeatherKafkaSenderService(@Value("${spring.kafka.topic.name}") String sendClientTopic,
                                     KafkaTemplate<String, MessageDTO> kafkaTemplate,
                                     BiConsumer<String,SendResult<String, MessageDTO>> successSendingHandler,
                                     BiConsumer<String,Throwable> errorSendingHandler) {
        this.sendClientTopic = sendClientTopic;
        this.kafkaTemplate = kafkaTemplate;
        this.successSendingHandler = successSendingHandler;
        this.errorSendingHandler = errorSendingHandler;
    }

    @Override
    public void produceMessage(String key, MessageDTO message) {
        try {
            var sendResult = kafkaTemplate.send(sendClientTopic, key, message);

            sendResult.whenComplete((result, ex) -> {
                if (isNull(ex)) {
                    if (nonNull(successSendingHandler))
                        successSendingHandler.accept(key, result);
                } else {
                    if (nonNull(errorSendingHandler))
                        errorSendingHandler.accept(key, ex);
                }
            });

        } catch (KafkaException ke) {

            throw new AppProducerException(ke.getCause());
        }

    }

}
