package ru.madmax.pet.microweather.producer.service.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microweather.producer.model.Weather;
import ru.madmax.pet.microweather.producer.model.WeatherBuilder;
import ru.madmax.pet.microweather.producer.service.LogService;
import ru.madmax.pet.microweather.producer.service.WeatherKafkaSenderService;

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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherKafkaSenderServiceVaiKafkaListenerAnnotationTest {

    final WeatherKafkaSenderService weatherSenderService;
    final KafkaTemplate<String, Weather> kafkaTemplate;
    BlockingQueue<ConsumerRecord<String, Weather>> records = new LinkedBlockingQueue<>();

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
        final String key = "kafka-annotation-1.1";
        weatherSenderService.produceWeather(key, weather);
        ConsumerRecord<String, Weather> consumerRecord = records.poll(1000, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key()).isEqualTo(key);
        assertThat(consumerRecord.value()).isEqualTo(weather);
        assertThat(records).isEmpty();

        verify(logService, times(1)).info(any(String.class));
        verify(logService, never()).error(any(Throwable.class));
    }

    @Test
    void sendTwoWeatherMessagesToProducerOneAfterAnother() throws InterruptedException {
        final Weather weather1 = WeatherBuilder.aWeather().withNow(1L).build();
        final Weather weather2 = WeatherBuilder.aWeather().withNow(2L).build();
        final String key1 = "kafka-annotation-2.1";
        final String key2 = "kafka-annotation-2.2";
        weatherSenderService.produceWeather(key1, weather1);
        weatherSenderService.produceWeather(key2, weather2);
        ConsumerRecord<String, Weather> consumerRecord1 = records.poll(3000, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord1).isNotNull();
        assertThat(consumerRecord1.key()).isEqualTo(key1);
        assertThat(consumerRecord1.value()).isEqualTo(weather1);

        ConsumerRecord<String, Weather> consumerRecord2 = records.poll(50, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord2).isNotNull();
        assertThat(consumerRecord2.key()).isEqualTo(key2);
        assertThat(consumerRecord2.value()).isEqualTo(weather2);

        assertThat(records).isEmpty();

        verify(logService, times(2)).info(any(String.class));
        verify(logService, never()).error(any(Throwable.class));
    }

    @Test
    void tryToSendToUnexistingTopic() throws InterruptedException {
        var producerServiceWithUnexistingTopic = new WeatherKafkaSenderService(
                "mock-topic",
                kafkaTemplate,
                logService);
        final Weather weather = WeatherBuilder.aWeather().build();
        final String key = "kafka-annotation-1.2";
        producerServiceWithUnexistingTopic.produceWeather(key, weather);
        ConsumerRecord<String, Weather> consumerRecord = records.poll(1000, TimeUnit.MILLISECONDS);

        assertThat(consumerRecord).isNull();
        assertThat(records).isEmpty();

        verify(logService, times(1)).info(any(String.class));
        verify(logService, never()).error(any(Throwable.class));

    }

}
