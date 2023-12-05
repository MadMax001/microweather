package ru.madmax.pet.microweather.producer.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.madmax.pet.microweather.producer.model.*;
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
                topics = "${spring.kafka.topic.name}"
)
@DirtiesContext
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherKafkaSenderServiceViaConsumerFactoryTest {
    final WeatherKafkaSenderService weatherSenderService;
    final ConsumerFactory<String, MessageDTO> consumerFactory;
    @Value("${spring.kafka.topic.name}")
    String testTopic;

    @SpyBean
    LogService logService;

    final ObjectMapper objectMapper;

    @Test
    void sendWeather_andGetItFromConsumer() throws JsonProcessingException {
        try (
                Consumer<String, MessageDTO> consumer = consumerFactory
                        .createConsumer("ConsumerFactoryTestGroup", null)
        ) {
            consumer.subscribe(Collections.singletonList(testTopic));

            final Weather weather = TestWeatherBuilder.aWeather().build();
            final MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                    .withType(MessageType.WEATHER)
                    .withMessage(objectMapper.writeValueAsString(weather))
                    .build();

            final String key = "consumer-factory-1";
            weatherSenderService.produceMessage(key, messageDTO);

            ConsumerRecords<String, MessageDTO> messages = consumer.poll(Duration.ofSeconds(15));

            assertThat(messages.count()).isEqualTo(1);
            assertThat(messages).singleElement().satisfies(singleRecord -> {
                assertThat(singleRecord.key()).isEqualTo(key);
                assertThat(singleRecord.value()).isNotNull();
                assertThat(singleRecord.value().getMessage()).isEqualTo(messageDTO.getMessage());
                assertThat(singleRecord.value().getType()).isEqualTo(MessageType.WEATHER);
            });

            verify(logService, times(1)).info(any(String.class));
            verify(logService, never()).error(any(Throwable.class));
            consumer.commitSync();
            consumer.unsubscribe();
        }
    }

}