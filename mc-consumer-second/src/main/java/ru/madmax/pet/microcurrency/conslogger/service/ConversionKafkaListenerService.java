package ru.madmax.pet.microcurrency.conslogger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;
import ru.madmax.pet.microcurrency.conslogger.service.handler.ConsumeHandler;


@Service
@RequiredArgsConstructor
public class ConversionKafkaListenerService implements ConversionListenerService {
    private final ConsumeHandler consumeHandler;
    private final LogService logService;

    @Override
    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            containerFactory = "listenerContainerFactory")
    public void listen(
            @Payload MessageDTO message,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        logService.info(key, String.format("Successful receive: %s%nMetadata: partition: %s, offset: %s",
                message,
                partition,
                offset));
        consumeHandler.accept(key, message);
    }
}
