package ru.madmax.pet.microweather.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;


public class AbstractContainersIntegrationTest {
    static Logger LOGGER = LoggerFactory.getLogger(AbstractContainersIntegrationTest.class);

    static PostgreSQLContainer<?> POSTGRES_SQL_CONTAINER;
    static KafkaContainer KAFKA_CONTAINER;

    static {
        KAFKA_CONTAINER =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

        POSTGRES_SQL_CONTAINER =
                new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.7-alpine"))
                        .withMinimumRunningDuration(Duration.ofSeconds(10))
                        .withReuse(true);

        POSTGRES_SQL_CONTAINER.setCommand("postgres", "-c", "fsync=off", "-c", "log_statement=all");

        Startables.deepStart(POSTGRES_SQL_CONTAINER, KAFKA_CONTAINER).join();

//        POSTGRES_SQL_CONTAINER.start();

        LOGGER.debug(POSTGRES_SQL_CONTAINER.getLogs());
        POSTGRES_SQL_CONTAINER.followOutput(new Slf4jLogConsumer(LOGGER));
    }

    @DynamicPropertySource
    static void overrideTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                String.format("r2dbc:pool:postgresql://%s:%d/%s",
                        POSTGRES_SQL_CONTAINER.getHost(),
                        POSTGRES_SQL_CONTAINER.getFirstMappedPort(),
                        POSTGRES_SQL_CONTAINER.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES_SQL_CONTAINER::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES_SQL_CONTAINER::getPassword);

        registry.add("spring.liquibase.url", POSTGRES_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.liquibase.user", POSTGRES_SQL_CONTAINER::getUsername);
        registry.add("spring.liquibase.password", POSTGRES_SQL_CONTAINER::getPassword);

        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }
}
