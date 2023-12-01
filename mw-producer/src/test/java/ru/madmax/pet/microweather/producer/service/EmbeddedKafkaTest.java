package ru.madmax.pet.microweather.producer.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}",
        partitions = 1
)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class EmbeddedKafkaTest {

    final KafkaAdmin admin;
    final EmbeddedKafkaBroker broker;


    @Test
    void checkBootstrapServersParameterResolutionExample(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers ) {
        assertThat(broker.getBrokersAsString()).isEqualTo(bootstrapServers);

    }

    @Test
    void checkTopicCreation(@Value("${spring.kafka.topic.name}") String topic) {
        assertThat(broker.getTopics()).hasSize(1);
        assertThat(broker.getTopics()).singleElement().isEqualTo(topic);

        assertThat(admin.describeTopics(topic)).containsKey(topic);
    }

    @Test
    void checkConfigurationProperties(@Value("${spring.kafka.client-id}") String clientId) {
        assertThat(admin.getConfigurationProperties()).containsEntry(
                AdminClientConfig.CLIENT_ID_CONFIG, clientId);
    }
}
