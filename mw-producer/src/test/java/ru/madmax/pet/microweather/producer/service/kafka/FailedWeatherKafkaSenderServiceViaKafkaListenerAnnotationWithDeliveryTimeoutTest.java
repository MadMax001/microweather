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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microweather.common.model.*;
import ru.madmax.pet.microweather.producer.service.LogService;
import ru.madmax.pet.microweather.producer.service.WeatherKafkaSenderService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
class FailedWeatherKafkaSenderServiceViaKafkaListenerAnnotationWithDeliveryTimeoutTest {
    final WeatherKafkaSenderService weatherSenderService;
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
    void sendWeatherMessageToProducer_AndProducerCantSendByTimeout_AndCheckOnFailedSection() throws InterruptedException, JsonProcessingException {
        doNothing().when(logService).error(anyString(),anyString());

        final Weather weather = TestWeatherBuilder.aWeather().build();
        final MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.WEATHER)
                .withMessage(objectMapper.writeValueAsString(weather))
                .build();
        final String key = "kafka-annotation-4";
        weatherSenderService.produceMessage(key, messageDTO);
        Thread.sleep(500);

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
