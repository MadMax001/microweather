package ru.madmax.pet.microweather.producer.service.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microweather.producer.model.Weather;
import ru.madmax.pet.microweather.producer.model.WeatherBuilder;
import ru.madmax.pet.microweather.producer.service.LogService;
import ru.madmax.pet.microweather.producer.service.WeatherKafkaSenderService;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
//@ContextConfiguration(classes = WeatherKafkaSenderServiceViaConsumerFactoryConfiguration.class)
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
                topics = "${spring.kafka.topic.name}",
                partitions = 1
)
@DirtiesContext
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherKafkaSenderServiceViaConsumerFactoryTest {
    private final WeatherKafkaSenderService weatherSenderService;
    private final ConsumerFactory<String, Weather> consumerFactory;
    @Value("${spring.kafka.topic.name}")
    String testTopic;

    @SpyBean
    LogService logService;

    @Test
    void sendWeather_andGetItFromConsumer() {
        try (
                Consumer<String, Weather> consumer = consumerFactory
                        .createConsumer("ConsumerFactoryTestGroup", null)
        ) {
            consumer.subscribe(Collections.singletonList(testTopic));

            final Weather weather = WeatherBuilder.aWeather().build();
            final String key = "consumer-factory-1";
            weatherSenderService.produceWeather(key, weather);

            ConsumerRecords<String, Weather> messages = consumer.poll(Duration.ofSeconds(3));

            assertThat(messages.count()).isEqualTo(1);
            assertThat(messages).singleElement().satisfies(singleRecord -> {
                assertThat(singleRecord.key()).isEqualTo(key);
                assertThat(singleRecord.value()).isEqualTo(weather);
            });

            verify(logService, times(1)).info(any(String.class));
            verify(logService, never()).error(any(Throwable.class));
            consumer.commitSync();
            consumer.unsubscribe();
        }
    }

}