package ru.madmax.pet.microweather.producer.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microweather.producer.model.*;
import ru.madmax.pet.microweather.producer.service.WeatherKafkaSenderService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
                topics = "${spring.kafka.topic.name}",
                partitions = 1


)
//@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)

class WeatherKafkaSenderServiceViaSetupMessageListenerTest {

    KafkaMessageListenerContainer<String, MessageDTO> container;
    BlockingQueue<ConsumerRecord<String, MessageDTO>> records;

    final WeatherKafkaSenderService weatherSenderService;
    final ConsumerFactory<String, MessageDTO> consumerFactory;
    final ObjectMapper objectMapper;

    @Value("${spring.kafka.topic.name}")
    String testTopic;

    @Value("${spring.kafka.replication.factor}")
    Integer replicationFactor;

    @BeforeAll
    void setUp() {
        ContainerProperties containerProperties = new ContainerProperties(testTopic);
        containerProperties.setGroupId("SetupMessageListenerTestGroup");

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        records = new LinkedBlockingQueue<>();

        container.setupMessageListener((MessageListener<String, MessageDTO>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, replicationFactor);
    }

    @AfterAll
    void tearDown() {
        container.stop();
    }

    @Test
    void sendWeatherMessageToProducer() throws InterruptedException, JsonProcessingException {
        final Weather weather = TestWeatherBuilder.aWeather().build();
        final MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.WEATHER)
                .withMessage(objectMapper.writeValueAsString(weather))
                .build();

        final String key = "message-listener-1";
        weatherSenderService.produceMessage(key, messageDTO);

        ConsumerRecord<String, MessageDTO> message = records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.key()).isEqualTo(key);
        assertThat(message.value()).isNotNull();
        assertThat(message.value().getMessage()).isEqualTo(messageDTO.getMessage());
        assertThat(message.value().getType()).isEqualTo(MessageType.WEATHER);

        assertThat(records).isEmpty();
        }


}