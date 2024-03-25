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
import org.springframework.boot.test.mock.mockito.MockBean;
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
@SpringBootTest(properties = {
        "spring.kafka.producer.properties[delivery.timeout.ms]=10",
        "spring.kafka.producer.properties[request.timeout.ms]=10",
        "spring.kafka.producer.properties[retries]=3"
})
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FailedCurrencyKafkaSenderServiceViaKafkaListenerAnnotationWithDeliveryTimeoutTest {
    final CurrencyKafkaSenderService currencySenderService;
    BlockingQueue<ConsumerRecord<String, MessageDTO>> records = new LinkedBlockingQueue<>();

    @MockBean
    LogService logService;


    final ObjectMapper objectMapper;

    @Captor
    ArgumentCaptor<String> errorMessageCaptor;

    @Captor
    ArgumentCaptor<String> keyCaptor;

    @KafkaListener(topics = "${spring.kafka.topic.name}",
            groupId = "FailedKafkaListenerAnnotationTestGroup1",
            containerFactory = "kafkaListenerContainerFactory")
    private void listen(ConsumerRecord<String, MessageDTO> consumerRecord) throws InterruptedException {
        records.put(consumerRecord);
    }

    @Test
    void sendConversionMessageToProducer_AndProducerCantSendByTimeout_AndCheckOnFailedSection() throws InterruptedException, JsonProcessingException {
        doNothing().when(logService).error(anyString(),anyString());

        final Conversion response = TestConversionBuilder.aConversion().build();
        final MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.CURRENCY)
                .withMessage(objectMapper.writeValueAsString(response))
                .build();
        final String key = "kafka-annotation-4";
        currencySenderService.produceMessage(key, messageDTO);
        records.poll(10, TimeUnit.SECONDS);

        verify(logService, times(1)).info(anyString(), anyString());
        verify(logService, times(1)).error(keyCaptor.capture(), errorMessageCaptor.capture());
        String errorMessage = errorMessageCaptor.getValue();
        String keyValue = keyCaptor.getValue();

        assertThat(errorMessage).contains(
                "KafkaProducerException",
                "TimeoutException");

        assertThat(keyValue).isEqualTo(key);
    }
}
