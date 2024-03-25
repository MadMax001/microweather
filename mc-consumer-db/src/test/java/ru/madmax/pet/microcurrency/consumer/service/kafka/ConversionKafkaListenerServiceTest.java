package ru.madmax.pet.microcurrency.consumer.service.kafka;

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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;
import ru.madmax.pet.microcurrency.common.model.TestConversionBuilder;
import ru.madmax.pet.microcurrency.common.model.TestMessageDTOBuilder;
import ru.madmax.pet.microcurrency.consumer.configuration.ConsumerBarrierReady;
import ru.madmax.pet.microcurrency.consumer.configuration.KafkaConfiguration;
import ru.madmax.pet.microcurrency.consumer.exception.AppConsumerException;
import ru.madmax.pet.microcurrency.consumer.service.LogService;
import ru.madmax.pet.microcurrency.consumer.service.Slf4JLogService;
import ru.madmax.pet.microcurrency.consumer.service.ConversionKafkaListenerService;
import ru.madmax.pet.microcurrency.consumer.service.ConversionListenerService;
import ru.madmax.pet.microcurrency.consumer.service.handler.ConsumeHandler;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ActiveProfiles("test")
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "spring.kafka.properties.isolation.level=read_committed",
        "spring.kafka.client-id=consumer-tester",
        "spring.kafka.topic.name=test-simple-topic",
        "spring.kafka.replication.factor=1",
        "spring.kafka.partition.number=1",
        "spring.kafka.consumer.group-id=mw-group-db"
})
@ContextConfiguration(classes = {
        ObjectMapper.class,
        ConsumerBarrierReady.class,
        KafkaProperties.class,
        KafkaConfiguration.class,
        Slf4JLogService.class,
        ConversionKafkaProducerTestConfiguration.class,
        ConsumeHandler.class,
        ConversionKafkaListenerService.class
})
@AutoConfigureWebTestClient
@EmbeddedKafka(
        bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}"
)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag("EmbeddedKafka+H2")
class ConversionKafkaListenerServiceTest {
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

    final ObjectMapper objectMapper;
    final KafkaTemplate<String, MessageDTO> kafkaTemplate;
    final ConversionListenerService conversionListenerService;
    final ConsumerBarrierReady consumerBarrierReady;
    ExecutorService service = Executors.newCachedThreadPool();

    @BeforeEach
    void setUp() throws InterruptedException {
        var waitingResult = consumerBarrierReady.await(30, TimeUnit.SECONDS);
        if (!waitingResult)
            throw new AppConsumerException(new RuntimeException("Kafka is not ready"));

    }

    @Test
    void sendTestMessage_andConsumeIt_AndCheckSuccessConsumeHandlerParameters_AndCountLogs()
            throws JsonProcessingException, ExecutionException, InterruptedException {

        var conversion = TestConversionBuilder.aConversion().build();
        var key = "test-consumer-1";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(conversion))
                .build();
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());
        CountDownLatch senderBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            senderBarrier.countDown();
            return null;
        }).when(consumeHandler).accept(anyString(), any());

        var task = createKafkaSenderTask(testTopic, key, messageDTO, senderBarrier);
        service.submit(task).get();


        verify(consumeHandler, times(1)).accept(keyCaptor.capture(), messageCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo(key);
        assertThat(messageCaptor.getValue()).isEqualTo(messageDTO);

        verify(logService, times(1)).info(anyString(), anyString());
        verify(logService, never()).error(anyString(), anyString());

    }

    @Test
    void sendTestMessageToWrongTopic_AndCheckEmptyConsumer_AndCountLogs()
            throws InterruptedException, JsonProcessingException {

        var conversion = TestConversionBuilder.aConversion().build();
        var key = "test-consumer-2";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(conversion))
                .build();
        CountDownLatch senderBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            senderBarrier.countDown();
            return null;
        }).when(consumeHandler).accept(anyString(), any());


        var task = createKafkaSenderTask("wrong-topic", key, messageDTO, senderBarrier);
        try {
            service.submit(task).get();
        } catch (ExecutionException e) {}

        verify(logService, never()).info(anyString(), anyString());
        verify(logService, never()).error(anyString(), anyString());
        verify(consumeHandler, never()).accept(keyCaptor.capture(), messageCaptor.capture());
    }

    @Test
    void concurrencySendTestMessages_andConsumeTheir_AndCheckSuccessConsumeHandlerParameters_AndCountLogs()
            throws JsonProcessingException, InterruptedException {
        var concurrency = 10;
        var keyPrefix = "concurrency-test-consumer";
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());
        CountDownLatch senderBarrier = new CountDownLatch(concurrency);
        doAnswer(inv -> {
            senderBarrier.countDown();
            return null;
        }).when(consumeHandler).accept(anyString(), any());


        List<Callable<SendResult<String, MessageDTO>>> taskList = new ArrayList<>();
        for(int i = 0; i < concurrency; i++) {
            var conversion = TestConversionBuilder.aConversion()
                    .withBaseAmount(new BigDecimal(i + 1))
                    .build();

            var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                    .withMessage(objectMapper.writeValueAsString(conversion))
                    .build();

            taskList.add(createKafkaSenderTask(testTopic, keyPrefix + i, messageDTO, senderBarrier));
        }
        List<Future<SendResult<String, MessageDTO>>> futures = service.invokeAll(taskList);
        assertThat(futures).hasSize(concurrency);

        verify(logService, times(concurrency)).info(anyString(), anyString());
        verify(logService, never()).error(anyString(), anyString());

        verify(consumeHandler, times(concurrency)).accept(keyCaptor.capture(), messageCaptor.capture());

        assertThat(new HashSet<>(keyCaptor.getAllValues())).hasSize(concurrency);
        assertThat(new HashSet<>(messageCaptor.getAllValues())).hasSize(concurrency);

        messageCaptor.getAllValues().forEach(System.out::println);
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