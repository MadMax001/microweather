package ru.madmax.pet.microweather.producer.service.kafka;

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
import ru.madmax.pet.microweather.producer.model.Weather;
import ru.madmax.pet.microweather.producer.model.WeatherBuilder;
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

    KafkaMessageListenerContainer<String, Weather> container;
    BlockingQueue<ConsumerRecord<String, Weather>> records;

    final WeatherKafkaSenderService weatherSenderService;
    final ConsumerFactory<String, Weather> consumerFactory;

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

        container.setupMessageListener((MessageListener<String, Weather>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, replicationFactor);
    }

    @AfterAll
    void tearDown() {
        container.stop();
    }

    @Test
    void sendWeatherMessageToProducer() throws InterruptedException {
        final Weather weather = WeatherBuilder.aWeather().build();
        final String key = "message-listener-1";
        weatherSenderService.produceWeather(key, weather);

        ConsumerRecord<String, Weather> message = records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.key()).isEqualTo(key);
        assertThat(message.value()).isEqualTo(weather);

        assertThat(records).isEmpty();
        }


}