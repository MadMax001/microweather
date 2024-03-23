package ru.madmax.pet.microcurrency.consumer.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;
import ru.madmax.pet.microcurrency.common.model.TestConversionBuilder;
import ru.madmax.pet.microcurrency.common.model.TestMessageDTOBuilder;
import ru.madmax.pet.microcurrency.consumer.AbstractContainersIntegrationTest;
import ru.madmax.pet.microcurrency.consumer.configuration.ConsumerBarrierReady;
import ru.madmax.pet.microcurrency.consumer.exception.AppConsumerException;
import ru.madmax.pet.microcurrency.consumer.model.ErrorEntity;
import ru.madmax.pet.microcurrency.consumer.model.ConversionEntity;
import ru.madmax.pet.microcurrency.consumer.repository.ErrorRepository;
import ru.madmax.pet.microcurrency.consumer.repository.ConversionRepository;
import ru.madmax.pet.microcurrency.consumer.service.LogService;
import ru.madmax.pet.microcurrency.consumer.service.handler.Hook;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static ru.madmax.pet.microcurrency.common.model.MessageType.ERROR;

@SpringBootTest(properties = {
        "spring.kafka.topic.name=test-simple-topic",
        "spring.kafka.consumer.group-id=mw-test",
        "spring.kafka.client-id=consumer-tester",
        "spring.kafka.replication.factor=1",
        "spring.kafka.partition.number=1",

        "spring.logging.level.org.springframework.r2dbc=DEBUG",
        "spring.logging.level.liquibase=TRACE",
        "logging.level.io.r2dbc.postgresql.QUERY=DEBUG",
        "logging.level.io.r2dbc.postgresql.PARAM=DEBUG"
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag("Containers")
class ConsumerServiceContainersIT extends AbstractContainersIntegrationTest {
    final ObjectMapper objectMapper;
    final KafkaTemplate<String, MessageDTO> kafkaTemplate;
    final ConsumerBarrierReady consumerBarrierReady;
    final ConversionRepository conversionRepository;
    final ErrorRepository errorRepository;
//    final SourceCacheStub sourceCache;

    @Value("${spring.kafka.topic.name}")
    String testTopic;

    @MockBean
    LogService logService;

    @MockBean(name="successfulCompletionHook")
    Hook<String> successfulCompletionHook;
    @MockBean(name="errorCompletionHook")
    Hook<Throwable> errorCompletionHook;

    ExecutorService service = Executors.newCachedThreadPool();

    @Captor
    ArgumentCaptor<String> stringCaptor;

    @BeforeEach
    void setUp() throws InterruptedException {
        var waitingResult = consumerBarrierReady.await(30, TimeUnit.SECONDS);
        if (!waitingResult)
            throw new AppConsumerException(new RuntimeException("Kafka is not ready"));
    }


    @Test
    void receiveConversionMessage_SaveToDB_ThenReadFromDB_AndCheckConversion_AndCountLogs()
            throws InterruptedException, JsonProcessingException, ExecutionException {
        var conversion = TestConversionBuilder.aConversion().build();
        var key = "integration-test-consumer-1";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(conversion))
                .build();

        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        CountDownLatch processBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            processBarrier.countDown();
            return null;
        }).when(successfulCompletionHook).accept(eq(key), anyString());


        var task = createKafkaSenderTask(testTopic, key, messageDTO, processBarrier);
        service.submit(task).get();
        Mono<ConversionEntity> currencyDBMono = conversionRepository.findById(key);
        StepVerifier.create(currencyDBMono)
                .assertNext(element -> {
                    assertThat(element.getId()).isEqualTo(key);
                    assertThat(element.getSourceId()).isEqualTo(1L);
                    assertThat(element.getBase()).isEqualTo(conversion.getBase());
                    assertThat(element.getConvert()).isEqualTo(conversion.getConvert());
                    assertThat(element.getBaseAmount()).isEqualByComparingTo(conversion.getBaseAmount());
                    assertThat(element.getConversionAmount()).isEqualByComparingTo(conversion.getConversionAmount());
                })
                .expectComplete()
                .verify();

        verify(logService, times(2)).info(anyString(), stringCaptor.capture());
        verify(logService, never()).error(anyString(), anyString());
        stringCaptor.getAllValues().forEach(System.out::println);
    }

    @Test
    void receiveErrorMessage_SaveToDB_ThenReadFromDB_AndCheckError_AndCountLogs()
            throws ExecutionException, InterruptedException {
        var key = "integration-test-consumer-2";
        var errorDetails = "error details";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(ERROR)
                .withMessage(errorDetails)
                .build();

        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        CountDownLatch processBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            processBarrier.countDown();
            return null;
        }).when(errorCompletionHook).accept(eq(key), any());

        var task = createKafkaSenderTask(testTopic, key, messageDTO, processBarrier);
        service.submit(task).get();
        Mono<ErrorEntity> errorDBMono = errorRepository.findById(key);
        StepVerifier.create(errorDBMono)
                .assertNext(element -> {
                    assertThat(element.getId()).isEqualTo(key);
                    assertThat(element.getDetails()).isEqualTo(errorDetails);
                })
                .expectComplete()
                .verify();

        verify(logService, times(2)).info(anyString(), stringCaptor.capture());
        verify(logService, never()).error(anyString(), anyString());
        stringCaptor.getAllValues().forEach(System.out::println);

    }

    @Test
    void receiveConversionMessage_WithVeryLongKeyValue_TryToSaveToDB_ThenCantReadFromDB_AndCountLogs()
            throws InterruptedException, JsonProcessingException, ExecutionException {
        var conversion = TestConversionBuilder.aConversion().build();
        var key = "integration-test-consumer-3___________________________________________________________________________________________________________________________________________";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(conversion))
                .build();

        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        CountDownLatch processBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            processBarrier.countDown();
            return null;
        }).when(errorCompletionHook).accept(eq(key), any());

        var task = createKafkaSenderTask(testTopic, key, messageDTO, processBarrier);
        service.submit(task).get();

        Mono<ConversionEntity> currencyDBMono = conversionRepository.findById(key);
        StepVerifier.create(currencyDBMono)
                .expectComplete()
                .verify();

        verify(logService, times(1)).info(anyString(), stringCaptor.capture());
        verify(logService, times(1)).error(anyString(), stringCaptor.capture());
        stringCaptor.getAllValues().forEach(System.out::println);
    }

    @Test
    void receiveErrorMessage__WithVeryLongKeyValue_TryToSaveToDB_ThenCantReadFromDB_AndCountLogs() throws ExecutionException, InterruptedException {
        var key = "integration-test-consumer-4___________________________________________________________________________________________________________________________________________";
        var errorDetails = "error details";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withType(ERROR)
                .withMessage(errorDetails)
                .build();

        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        CountDownLatch processBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            processBarrier.countDown();
            return null;
        }).when(errorCompletionHook).accept(eq(key), any());

        var task = createKafkaSenderTask(testTopic, key, messageDTO, processBarrier);
        service.submit(task).get();

        Mono<ErrorEntity> errorDBMono = errorRepository.findById(key);
        StepVerifier.create(errorDBMono)
                .expectComplete()
                .verify();

        verify(logService, times(1)).info(anyString(), stringCaptor.capture());
        verify(logService, times(1)).error(anyString(), stringCaptor.capture());
        stringCaptor.getAllValues().forEach(System.out::println);
    }

    @Test
    void concurrency() throws JsonProcessingException, InterruptedException {
        var concurrency = 100;
        var keyPrefix = "concurrency-integration-";
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());
        CountDownLatch senderBarrier = new CountDownLatch(concurrency);

        CountDownLatch processBarrier = new CountDownLatch(10);
        doAnswer(inv -> {
            processBarrier.countDown();
            return null;
        }).when(successfulCompletionHook).accept(anyString(), anyString());

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

        Flux<ConversionEntity> currencies = Flux.concat(IntStream.range(0, concurrency)
                .boxed()
                .map(i -> conversionRepository.findById(keyPrefix + i))
                .collect(Collectors.toList()));

        StepVerifier.create(currencies)
                .expectNextCount(concurrency)
                .expectComplete()
                .verify();

        verify(logService, times(2 * concurrency)).info(anyString(), stringCaptor.capture());
        verify(logService, never()).error(anyString(), anyString());
        stringCaptor.getAllValues().forEach(System.out::println);
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
