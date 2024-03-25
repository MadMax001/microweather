package ru.madmax.pet.microcurrency.conslogger.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;
import ru.madmax.pet.microcurrency.common.model.TestConversionBuilder;
import ru.madmax.pet.microcurrency.common.model.TestMessageDTOBuilder;
import ru.madmax.pet.microcurrency.conslogger.AbstractContainersIntegrationTest;
import ru.madmax.pet.microcurrency.conslogger.configuration.ConsumerBarrierReady;
import ru.madmax.pet.microcurrency.conslogger.configuration.KafkaConfiguration;
import ru.madmax.pet.microcurrency.conslogger.exception.AppConsumerException;
import ru.madmax.pet.microcurrency.conslogger.service.LogService;
import ru.madmax.pet.microcurrency.conslogger.service.Slf4JLogService;
import ru.madmax.pet.microcurrency.conslogger.service.ConversionKafkaListenerService;
import ru.madmax.pet.microcurrency.conslogger.service.ConversionListenerService;
import ru.madmax.pet.microcurrency.conslogger.service.handler.ConsumeHandler;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ActiveProfiles("test")
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "spring.kafka.properties.isolation.level=read_committed",
        "spring.kafka.client-id=consumer-container-tester",
        "spring.kafka.topic.name=test-container-topic",
        "spring.kafka.replication.factor=1",
        "spring.kafka.partition.number=1",
        "spring.kafka.consumer.group-id=mc-group-db"
})
@ContextConfiguration(classes = {
        ObjectMapper.class,
        ConsumerBarrierReady.class,
        KafkaProperties.class,
        KafkaConfiguration.class,
        Slf4JLogService.class,
        ConversionKafkaProducerTestConfiguration.class,
        ConversionKafkaConsumerTestConfiguration.class,
        ConsumeHandler.class,
        ConversionKafkaListenerService.class
})
@AutoConfigureWebTestClient
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag("Containers")
class ConversionKafkaListenerServiceContainerTest extends AbstractContainersIntegrationTest {
    @Value("${spring.kafka.topic.name}")
    String testTopic;

    @MockBean
    LogService logService;

    @MockBean
    ConsumeHandler consumeHandler;

    @Captor
    ArgumentCaptor<String> keyCaptor;

    @Captor
    ArgumentCaptor<MessageDTO> messageCaptor;

    @Captor
    ArgumentCaptor<String> logMessage;

    final ObjectMapper objectMapper;
    final KafkaTemplate<String, MessageDTO> kafkaTemplate;
    final ConversionListenerService conversionListenerService;
    final ConsumerBarrierReady consumerBarrierReady;
    final ConversionKafkaConsumerTestConfiguration.SecondConsumerBarrierReady secondConsumerBarrierReady;
    ExecutorService service = Executors.newCachedThreadPool();

    @BeforeEach
    void setUp() throws InterruptedException {
        var waitingResult1 = consumerBarrierReady.await(30, TimeUnit.SECONDS);
        var waitingResult2 = secondConsumerBarrierReady.await(30, TimeUnit.SECONDS);

        if (!(waitingResult1 && waitingResult2))
            throw new AppConsumerException(new RuntimeException("Kafka is not ready"));

    }


    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            containerFactory = "secondListenerContainerFactory")
    public void listen(
            @Payload MessageDTO message,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        logService.info(key, String.format("[SECOND]Successful receive: %s%nMetadata: partition: %s, offset: %s",
                message,
                partition,
                offset));
        consumeHandler.accept(key, message);
    }

    @Test
    void sendTestMessage_andConsumeItByTwoConsumers_AndCheckSuccessConsumeHandlerParameters_AndCountLogs()
            throws JsonProcessingException, ExecutionException, InterruptedException {

        var conversion = TestConversionBuilder.aConversion().build();
        var key = "test-consumer-1";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(conversion))
                .build();
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());
        CountDownLatch senderBarrier = new CountDownLatch(2);
        doAnswer(inv -> {
            senderBarrier.countDown();
            return null;
        }).when(consumeHandler).accept(anyString(), any());

        var task = createKafkaSenderTask(testTopic, key, messageDTO, senderBarrier);
        service.submit(task).get();


        verify(consumeHandler, times(2)).accept(keyCaptor.capture(), messageCaptor.capture());
        List<String> allKeys = keyCaptor.getAllValues();
        for (String receivedKey : allKeys) {
            assertThat(receivedKey).isEqualTo(key);
        }
        List<MessageDTO> messages = messageCaptor.getAllValues();
        for (MessageDTO receivedMessage : messages) {
            assertThat(receivedMessage).isEqualTo(messageDTO);
        }
        verify(logService, times(2)).info(anyString(), logMessage.capture());
        verify(logService, never()).error(anyString(), anyString());

        List<String> logMessages = logMessage.getAllValues();

        var messageCountFromSecondListener = logMessages.stream().filter(message -> message.startsWith("[SECOND]")).count();
        assertThat(messageCountFromSecondListener).isOne();

    }

    private Callable<SendResult<String, MessageDTO>> createKafkaSenderTask
            (String topicName, String key, MessageDTO message, CountDownLatch senderBarrier) {
        return () -> {

            var stringMessageDTOSendResult =
                    kafkaTemplate.send(topicName, key, message);
            var senderSuccess = senderBarrier.await(2, TimeUnit.SECONDS);
            if (!senderSuccess)
                throw new RuntimeException("Can't transfer message");
            return stringMessageDTOSendResult.get();
        };
    }

}
