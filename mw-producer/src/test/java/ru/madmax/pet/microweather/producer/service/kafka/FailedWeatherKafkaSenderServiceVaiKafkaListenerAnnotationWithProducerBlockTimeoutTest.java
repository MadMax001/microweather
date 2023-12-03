package ru.madmax.pet.microweather.producer.service.kafka;

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
import ru.madmax.pet.microweather.producer.model.Weather;
import ru.madmax.pet.microweather.producer.model.WeatherBuilder;
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
        topics = "${spring.kafka.topic.name}",
        partitions = 1
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FailedWeatherKafkaSenderServiceVaiKafkaListenerAnnotationWithProducerBlockTimeoutTest {
    final WeatherKafkaSenderService weatherSenderService;
    BlockingQueue<ConsumerRecord<String, Weather>> records = new LinkedBlockingQueue<>();

    @SpyBean
    LogService logService;

    @KafkaListener(topics = "${spring.kafka.topic.name}",
            groupId = "FailedKafkaListenerAnnotationTestGroup",
            containerFactory = "kafkaListenerContainerFactory")
    private void listen(ConsumerRecord<String, Weather> consumerRecord) throws InterruptedException {
        records.put(consumerRecord);
    }

    @Test
    void sendWeatherMessageToProducer_AndProducerBlockVeryLong_AndThrowAppProducerException() {
        final Weather weather = WeatherBuilder.aWeather().build();
        final String key = "kafka-annotation-3";
        assertThatThrownBy(() -> weatherSenderService.produceWeather(key, weather))
                .isInstanceOf(AppProducerException.class)
                .message().contains("TimeoutException");

        verify(logService, never()).info(any(String.class));
        verify(logService, never()).error(any(Throwable.class));
    }
}
