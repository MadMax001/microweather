package ru.madmax.pet.microweather.producer.service.kafka;

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
import ru.madmax.pet.microweather.producer.model.Weather;
import ru.madmax.pet.microweather.producer.model.WeatherBuilder;
import ru.madmax.pet.microweather.producer.service.LogService;
import ru.madmax.pet.microweather.producer.service.WeatherKafkaSenderService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.kafka.producer.properties[delivery.timeout.ms]=10",
        "spring.kafka.producer.properties[request.timeout.ms]=10",
        "spring.kafka.producer.properties[retries]=3"
})
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}",
        partitions = 1
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FailedWeatherKafkaSenderServiceVaiKafkaListenerAnnotationWithDeliveryTimeoutTest {
    final WeatherKafkaSenderService weatherSenderService;
    BlockingQueue<ConsumerRecord<String, Weather>> records = new LinkedBlockingQueue<>();

    @MockBean
    LogService logService;

    @Captor
    ArgumentCaptor<String> errorMessageCaptor;


    @KafkaListener(topics = "${spring.kafka.topic.name}",
            groupId = "FailedKafkaListenerAnnotationTestGroup",
            containerFactory = "kafkaListenerContainerFactory")
    private void listen(ConsumerRecord<String, Weather> consumerRecord) throws InterruptedException {
        records.put(consumerRecord);
    }

    @Test
    void sendWeatherMessageToProducer_AndProducerCantSendByTimeout_AndCheckOnFailedSection() throws InterruptedException {
        doNothing().when(logService).error(any(String.class));

        final Weather weather = WeatherBuilder.aWeather().build();
        final String key = "kafka-annotation-4";
        weatherSenderService.produceWeather(key, weather);
        Thread.sleep(500);

        verify(logService, never()).info(any(String.class));
        verify(logService, times(1)).error(errorMessageCaptor.capture());
        String errorMessage = errorMessageCaptor.getValue();
        assertThat(errorMessage).contains(
                key,
                "KafkaProducerException",
                "TimeoutException");
    }
}
