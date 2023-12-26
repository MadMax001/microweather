package ru.madmax.pet.microweather.producer.service.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import ru.madmax.pet.microweather.common.model.MessageDTO;
import ru.madmax.pet.microweather.producer.service.LogService;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class SuccessSendingHandler implements BiConsumer<String, SendResult<String, MessageDTO>> {
    private final LogService logService;

    @Override
    public void accept(String key, SendResult<String, MessageDTO> result) {
        logService.info(String.format("Successful sending [%s]: %s%nMetadata: partition: %s, offset: %s",
                key,
                result.getProducerRecord().value(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset()));
    }


}
