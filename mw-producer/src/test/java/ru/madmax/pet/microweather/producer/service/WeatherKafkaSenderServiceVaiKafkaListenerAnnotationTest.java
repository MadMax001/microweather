package ru.madmax.pet.microweather.producer.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microweather.producer.model.Weather;
import ru.madmax.pet.microweather.producer.model.WeatherBuilder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}",
        partitions = 1
)
@DirtiesContext
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherKafkaSenderServiceVaiKafkaListenerAnnotationTest {

    final WeatherKafkaSenderService weatherSenderService;
    BlockingQueue<ConsumerRecord<String, Weather>> records = new LinkedBlockingQueue<>();
    final EmbeddedKafkaBroker embeddedKafkaBroker;
    final KafkaAdmin admin;

    @SpyBean
    LogService logService;

    @KafkaListener(topics = "${spring.kafka.topic.name}",
            groupId = "KafkaListenerAnnotationTestGroup",
            containerFactory = "kafkaListenerContainerFactory")
    private void listen(ConsumerRecord<String, Weather> consumerRecord) throws InterruptedException {
        records.put(consumerRecord);
    }



    @Test
    void sendWeatherMessageToProducer() throws InterruptedException {
        final Weather weather = WeatherBuilder.aWeather().build();
        final String key = "kafka-annotation-1";
        weatherSenderService.produceWeather(key, weather);
        ConsumerRecord<String, Weather> consumerRecord = records.poll(3000, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key()).isEqualTo(key);
        assertThat(consumerRecord.value()).isEqualTo(weather);
        assertThat(records).isEmpty();

        verify(logService, times(1)).info(any(String.class));
        verify(logService, never()).error(any(Throwable.class));
    }
}
