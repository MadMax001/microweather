package ru.madmax.pet.microcurrency.producer.service.kafka;

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
import ru.madmax.pet.microcurrency.common.model.*;
import ru.madmax.pet.microcurrency.producer.service.CurrencyKafkaSenderService;

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

class CurrencyKafkaSenderServiceViaSetupMessageListenerTest {

    KafkaMessageListenerContainer<String, MessageDTO> container;
    BlockingQueue<ConsumerRecord<String, MessageDTO>> records;

    final CurrencyKafkaSenderService currencySenderService;
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
    void sendConversionMessageToProducer() throws InterruptedException, JsonProcessingException {
        final Conversion response = TestConversionBuilder.aConversion().build();
        final MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(MessageType.CURRENCY)
                .withMessage(objectMapper.writeValueAsString(response))
                .build();

        final String key = "message-listener-1";
        currencySenderService.produceMessage(key, messageDTO);

        ConsumerRecord<String, MessageDTO> message = records.poll(15000, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.key()).isEqualTo(key);
        assertThat(message.value()).isNotNull();
        assertThat(message.value().getMessage()).isEqualTo(messageDTO.getMessage());
        assertThat(message.value().getType()).isEqualTo(MessageType.CURRENCY);

        assertThat(records).isEmpty();
        }


}