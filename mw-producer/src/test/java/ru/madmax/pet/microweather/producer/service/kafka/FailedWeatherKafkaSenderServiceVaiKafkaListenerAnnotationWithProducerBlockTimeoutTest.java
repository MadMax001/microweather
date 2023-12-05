package ru.madmax.pet.microweather.producer.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microweather.producer.exception.AppProducerException;
import ru.madmax.pet.microweather.producer.model.*;
import ru.madmax.pet.microweather.producer.service.LogService;
import ru.madmax.pet.microweather.producer.service.WeatherKafkaSenderService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(properties = { "spring.kafka.producer.properties[max.block.ms]=1" })
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FailedWeatherKafkaSenderServiceVaiKafkaListenerAnnotationWithProducerBlockTimeoutTest {
    final WeatherKafkaSenderService weatherSenderService;
    BlockingQueue<ConsumerRecord<String, MessageDTO>> records = new LinkedBlockingQueue<>();

    @SpyBean
    LogService logService;


    final ObjectMapper objectMapper;

    @KafkaListener(topics = "${spring.kafka.topic.name}",
            groupId = "FailedKafkaListenerAnnotationTestGroup",
            containerFactory = "kafkaListenerContainerFactory")
    private void listen(ConsumerRecord<String, MessageDTO> consumerRecord) throws InterruptedException {
        records.put(consumerRecord);
    }

    @Test
    void sendWeatherMessageToProducer_AndProducerBlockVeryLong_AndThrowAppProducerException() throws JsonProcessingException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.WEATHER)
                .withMessage(objectMapper.writeValueAsString(weather))
                .build();

        final String key = "kafka-annotation-3";
        assertThatThrownBy(() -> weatherSenderService.produceMessage(key, messageDTO))
                .isInstanceOf(AppProducerException.class)
                .message().contains("TimeoutException");

        verify(logService, never()).info(any(String.class));
        verify(logService, never()).error(any(Throwable.class));
    }
}
