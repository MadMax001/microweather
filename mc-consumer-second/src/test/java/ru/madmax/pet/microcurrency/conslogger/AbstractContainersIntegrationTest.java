package ru.madmax.pet.microcurrency.conslogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;


public class AbstractContainersIntegrationTest {
    static Logger LOGGER = LoggerFactory.getLogger(AbstractContainersIntegrationTest.class);

    static KafkaContainer KAFKA_CONTAINER;

    static {
        KAFKA_CONTAINER =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
        KAFKA_CONTAINER.start();
    }

    @DynamicPropertySource
    static void overrideTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }
}
