package ru.madmax.pet.microweather.producer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microweather.producer.model.Weather;
import ru.madmax.pet.microweather.producer.model.WeatherBuilder;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
//@DirtiesContext
@EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@TestPropertySource(properties = "spring.kafka.consumer.auto-offset-reset = earliest")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherKafkaSenderServiceTest {
    private final EmbeddedKafkaBroker embeddedKafkaBroker;
    private final WeatherKafkaSenderService weatherSenderService;
    private final ObjectMapper objectMapper;

    private BlockingQueue<ConsumerRecord<String, Weather>> records;

    private KafkaMessageListenerContainer<String, Weather> container;




    private Map<String, Object> getConsumerProperties() {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.GROUP_ID_CONFIG, "producer-test-group",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true",
                ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10",
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "10000",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

    }

    @BeforeAll
    void setUp() {
        DefaultKafkaConsumerFactory<String, Weather> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());
        ContainerProperties containerProperties = new ContainerProperties("test-simple-01");
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, Weather>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterAll
    void tearDown() {
        container.stop();
    }

    @Test
    void sendWeatherMessageToProducer() throws InterruptedException {
        final Weather weather = WeatherBuilder.aWeather().build();
        final String key = "12345";
        weatherSenderService.produceWeather(key, weather);

        ConsumerRecord<String, Weather> message = records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.key()).isEqualTo(key);
        assertThat(message.value()).isEqualTo(weather);
        }


/*
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        Producer<String, String> producer =
                new DefaultKafkaProducerFactory<String, String>(configs, new StringSerializer(), new StringSerializer())
                        .createProducer();

        Weather weather = WeatherBuilder.aWeather().build();
        // Act
        producer.send(new ProducerRecord<>("test-simple-01", "my-aggregate-id", "{\"event\":\"Test Event\"}"));
        producer.flush();

        // Assert
        ConsumerRecord<String, String> singleRecord = records.poll(100, TimeUnit.MILLISECONDS);
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.key()).isEqualTo("my-aggregate-id");
        assertThat(singleRecord.value()).isEqualTo("{\"event\":\"Test Event\"}");
*/


}