package ru.madmax.pet.microweather.producer.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microweather.producer.model.*;
import ru.madmax.pet.microweather.producer.service.LogService;
import ru.madmax.pet.microweather.producer.service.WeatherKafkaSenderService;
import ru.madmax.pet.microweather.producer.service.handlers.ErrorSendingHandler;
import ru.madmax.pet.microweather.producer.service.handlers.SuccessSendingHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherKafkaSenderServiceVaiKafkaListenerAnnotationTest {

    final WeatherKafkaSenderService weatherSenderService;
    final KafkaTemplate<String, MessageDTO> kafkaTemplate;
    final SuccessSendingHandler successSendingHandler;
    final ErrorSendingHandler errorSendingHandler;
    BlockingQueue<ConsumerRecord<String, MessageDTO>> records = new LinkedBlockingQueue<>();

    @SpyBean
    LogService logService;

    @Captor
    ArgumentCaptor<String> logInfoCaptor;

    final ObjectMapper objectMapper;

    @KafkaListener(topics = "${spring.kafka.topic.name}",
            groupId = "KafkaListenerAnnotationTestGroup",
            containerFactory = "kafkaListenerContainerFactory")
    private void listen(ConsumerRecord<String, MessageDTO> consumerRecord) throws InterruptedException {
        records.put(consumerRecord);
    }



    @Test
    void sendWeatherMessageToProducer_AndConsumerGetMessage() throws InterruptedException, JsonProcessingException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.WEATHER)
                .withMessage(objectMapper.writeValueAsString(weather))
                .build();

        final String key = "kafka-annotation-1.1";
        weatherSenderService.produceMessage(key, messageDTO);
        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(15000, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key()).isEqualTo(key);
        assertThat(consumerRecord.value()).isNotNull();
        assertThat(consumerRecord.value().getMessage()).isEqualTo(messageDTO.getMessage());
        assertThat(consumerRecord.value().getType()).isEqualTo(MessageType.WEATHER);
        assertThat(records).isEmpty();

        verify(logService, times(1)).info(logInfoCaptor.capture());
        String logInfoString = logInfoCaptor.getValue();
        assertThat(logInfoString).contains(
                "Successful sending",
                key,
                messageDTO.getMessage()
        );
        verify(logService, never()).error(any(Throwable.class));
    }

    @Test
    void sendTwoWeatherMessagesToProducerOneAfterAnother_andConsumerGetTwoMessages() throws InterruptedException, JsonProcessingException {
        final Weather weather1 = TestWeatherBuilder.aWeather().withNow(1L).build();
        final MessageDTO messageDTO1 = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.WEATHER)
                .withMessage(objectMapper.writeValueAsString(weather1))
                .build();

        final Weather weather2 = TestWeatherBuilder.aWeather().withNow(2L).build();
        final MessageDTO messageDTO2 = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.WEATHER)
                .withMessage(objectMapper.writeValueAsString(weather2))
                .build();

        final String key1 = "kafka-annotation-2.1";
        final String key2 = "kafka-annotation-2.2";


        weatherSenderService.produceMessage(key1, messageDTO1);
        weatherSenderService.produceMessage(key2, messageDTO2);
        ConsumerRecord<String, MessageDTO> consumerRecord1 = records.poll(15000, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord1).isNotNull();
        assertThat(consumerRecord1.key()).isEqualTo(key1);
        assertThat(consumerRecord1.value()).isNotNull();
        assertThat(consumerRecord1.value().getMessage()).isEqualTo(messageDTO1.getMessage());
        assertThat(consumerRecord1.value().getType()).isEqualTo(MessageType.WEATHER);

        ConsumerRecord<String, MessageDTO> consumerRecord2 = records.poll(50, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord2).isNotNull();
        assertThat(consumerRecord2.key()).isEqualTo(key2);
        assertThat(consumerRecord2.value()).isNotNull();
        assertThat(consumerRecord2.value().getMessage()).isEqualTo(messageDTO2.getMessage());
        assertThat(consumerRecord2.value().getType()).isEqualTo(MessageType.WEATHER);

        assertThat(records).isEmpty();

        verify(logService, times(2)).info(logInfoCaptor.capture());
        var logInfoStrings = logInfoCaptor.getAllValues();
        assertThat(logInfoStrings.get(0)).contains(
                "Successful sending",
                key1,
                messageDTO1.getMessage()
        );
        assertThat(logInfoStrings.get(1)).contains(
                "Successful sending",
                key2,
                messageDTO2.getMessage()
        );
        verify(logService, never()).error(any(Throwable.class));    }

    @Test
    void tryToSendToUnexistingTopic_andConsumerDoesNotGetAnyMessage() throws InterruptedException, JsonProcessingException {
        var producerServiceWithUnexistingTopic = new WeatherKafkaSenderService(
                "mock-topic",
                kafkaTemplate,
                successSendingHandler,
                errorSendingHandler);
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.WEATHER)
                .withMessage(objectMapper.writeValueAsString(weather))
                .build();

        final String key = "kafka-annotation-3.1";
        producerServiceWithUnexistingTopic.produceMessage(key, messageDTO);
        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(15000, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord).isNull();
        assertThat(records).isEmpty();

        verify(logService, times(1)).info(any(String.class));
        verify(logService, never()).error(any(Throwable.class));

    }

    @Test
    void sendTwoWeatherMessagesWitSameKeysToProducer_andConsumeTwoMessagesInSameOrder() throws InterruptedException, JsonProcessingException {
        final Weather weather1 = TestWeatherBuilder.aWeather().withNow(1L).build();
        final MessageDTO messageDTO1 = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.WEATHER)
                .withMessage(objectMapper.writeValueAsString(weather1))
                .build();

        final Weather weather2 = TestWeatherBuilder.aWeather().withNow(2L).build();
        final MessageDTO messageDTO2 = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.WEATHER)
                .withMessage(objectMapper.writeValueAsString(weather2))
                .build();

        final String key = "kafka-annotation-4.1";
        weatherSenderService.produceMessage(key, messageDTO1);
        weatherSenderService.produceMessage(key, messageDTO2);
        ConsumerRecord<String, MessageDTO> consumerRecord1 = records.poll(15000, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord1).isNotNull();
        assertThat(consumerRecord1.key()).isEqualTo(key);
        assertThat(consumerRecord1.value()).isNotNull();
        assertThat(consumerRecord1.value().getMessage()).isEqualTo(messageDTO1.getMessage());
        assertThat(consumerRecord1.value().getType()).isEqualTo(MessageType.WEATHER);

        ConsumerRecord<String, MessageDTO> consumerRecord2 = records.poll(50, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord2).isNotNull();
        assertThat(consumerRecord2.key()).isEqualTo(key);
        assertThat(consumerRecord2.value()).isNotNull();
        assertThat(consumerRecord2.value().getMessage()).isEqualTo(messageDTO2.getMessage());
        assertThat(consumerRecord2.value().getType()).isEqualTo(MessageType.WEATHER);

        assertThat(records).isEmpty();

        verify(logService, times(2)).info(logInfoCaptor.capture());
        var logInfoStrings = logInfoCaptor.getAllValues();
        assertThat(logInfoStrings.get(0)).contains(
                "Successful sending",
                key,
                messageDTO1.getMessage()
        );
        assertThat(logInfoStrings.get(1)).contains(
                "Successful sending",
                key,
                messageDTO2.getMessage()
        );
        verify(logService, never()).error(any(Throwable.class));

    }
}
