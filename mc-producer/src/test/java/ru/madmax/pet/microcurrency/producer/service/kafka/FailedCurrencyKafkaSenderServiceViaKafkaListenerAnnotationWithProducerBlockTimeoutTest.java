package ru.madmax.pet.microcurrency.producer.service.kafka;

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
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microcurrency.common.model.*;
import ru.madmax.pet.microcurrency.producer.service.LogService;
import ru.madmax.pet.microcurrency.producer.service.CurrencyKafkaSenderService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(properties = { "spring.kafka.producer.properties[max.block.ms]=1" })
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FailedCurrencyKafkaSenderServiceViaKafkaListenerAnnotationWithProducerBlockTimeoutTest {
    final CurrencyKafkaSenderService currencySenderService;
    BlockingQueue<ConsumerRecord<String, MessageDTO>> records = new LinkedBlockingQueue<>();

    @SpyBean
    LogService logService;

    @Captor
    ArgumentCaptor<String> infoCaptor;

    final ObjectMapper objectMapper;

    @KafkaListener(topics = "${spring.kafka.topic.name}",
            groupId = "FailedKafkaListenerAnnotationTestGroup2",
            containerFactory = "kafkaListenerContainerFactory")
    private void listen(ConsumerRecord<String, MessageDTO> consumerRecord) throws InterruptedException {
        records.put(consumerRecord);
    }

    @Test
    void sendConversionMessageToProducer_AndProducerBlockVeryLong_AndThrowAppProducerException()
            throws JsonProcessingException, InterruptedException {
        final Conversion response = TestConversionBuilder.aConversion().build();
        final MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.CURRENCY)
                .withMessage(objectMapper.writeValueAsString(response))
                .build();

        final String key = "kafka-annotation-3";
        currencySenderService.produceMessage(key, messageDTO);
        records.poll(10, TimeUnit.SECONDS);

        verify(logService, times(1)).info(anyString(), infoCaptor.capture());
        verify(logService, times(1)).error(anyString(), infoCaptor.capture());

        assertThat(infoCaptor.getAllValues().get(1)).contains(
                "org.springframework.kafka.KafkaException",
                "org.apache.kafka.common.errors.TimeoutException"
        );
        infoCaptor.getAllValues().forEach(System.out::println);
    }
}
