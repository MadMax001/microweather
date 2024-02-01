package ru.madmax.pet.microweather.consumer.integration;

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
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.madmax.pet.microweather.common.model.MessageDTO;
import ru.madmax.pet.microweather.common.model.TestMessageDTOBuilder;
import ru.madmax.pet.microweather.common.model.TestWeatherBuilder;
import ru.madmax.pet.microweather.consumer.exception.AppConsumerException;
import ru.madmax.pet.microweather.consumer.configuration.ConsumerBarrierReady;
import ru.madmax.pet.microweather.consumer.model.ErrorDomain;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;
import ru.madmax.pet.microweather.consumer.repository.ErrorRepository;
import ru.madmax.pet.microweather.consumer.repository.WeatherRepository;
import ru.madmax.pet.microweather.consumer.service.LogService;
import ru.madmax.pet.microweather.consumer.service.handler.Hook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static ru.madmax.pet.microweather.common.model.MessageType.ERROR;

@SpringBootTest(properties = {
        "spring.kafka.topic.name=test-simple-topic",
        "spring.kafka.consumer.group-id=mw-test",
        "spring.kafka.client-id=consumer-tester",
        "spring.kafka.replication.factor=1",
        "spring.kafka.partition.number=1",

//        "spring.r2dbc.url=r2dbc:h2:mem:///~/db/weather;DB_CLOSE_DELAY=-1",
//        "spring.r2dbc.username=sa",
//        "spring.r2dbc.password=null",
//
//        "spring.r2dbc.url=r2dbc:h2:mem:///~/db/weather;DB_CLOSE_DELAY=-1",
//        "spring.r2dbc.username=sa",
//        "spring.r2dbc.password=null",

        "spring.logging.level.org.springframework.r2dbc=DEBUG",
        "spring.logging.level.liquibase=TRACE",
        "logging.level.io.r2dbc.h2=DEBUG"
})
@EmbeddedKafka(
        bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}"
)
@ActiveProfiles("test")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag("EmbeddedKafka+H2")
class ConsumerServiceWithH2AndEmbeddedKafkaIT {
    final ObjectMapper objectMapper;
    final KafkaTemplate<String, MessageDTO> kafkaTemplate;
    final ConsumerBarrierReady consumerBarrierReady;
    final WeatherRepository weatherRepository;
    final ErrorRepository errorRepository;

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
    void receiveWeatherMessage_SaveToDB_ThenReadFromDB_AndCheckWeather_AndCountLogs()
            throws InterruptedException, JsonProcessingException, ExecutionException {
        var weather = TestWeatherBuilder.aWeather().build();
        var key = "integration-test-consumer-1";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(weather))
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
        Mono<WeatherDomain> weatherDBMono = weatherRepository.findById(key);
        StepVerifier.create(weatherDBMono)
                .assertNext(element -> {
                    assertThat(element.getId()).isEqualTo(key);
                    assertThat(element.getNow()).isEqualTo(weather.getNow());
                    assertThat(element.getTemperature()).isEqualTo(weather.getFact().getTemp(), withPrecision(2d));
                    assertThat(element.getWind()).isEqualTo(weather.getFact().getWindSpeed(), withPrecision(2d));
                    assertThat(element.getUrl()).isEqualTo(weather.getInfo().getUrl());
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
        Mono<ErrorDomain> errorDBMono = errorRepository.findById(key);
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
    void receiveWeatherMessage_WithVeryLongKeyValue_TryToSaveToDB_ThenCantReadFromDB_AndCountLogs()
            throws InterruptedException, JsonProcessingException, ExecutionException {
        var weather = TestWeatherBuilder.aWeather().build();
        var key = "integration-test-consumer-3___________________________________________________________________________________________________________________________________________";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(weather))
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

        Mono<WeatherDomain> weatherDBMono = weatherRepository.findById(key);
        StepVerifier.create(weatherDBMono)
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

        Mono<ErrorDomain> errorDBMono = errorRepository.findById(key);
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
            var weather = TestWeatherBuilder.aWeather()
                    .withNow(i)
                    .build();

            var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                    .withMessage(objectMapper.writeValueAsString(weather))
                    .build();

            taskList.add(createKafkaSenderTask(testTopic, keyPrefix + i, messageDTO, senderBarrier));
        }
        List<Future<SendResult<String, MessageDTO>>> futures = service.invokeAll(taskList);
        assertThat(futures).hasSize(concurrency);

        Flux<WeatherDomain> weathers = Flux.concat(IntStream.range(0, concurrency)
                .boxed()
                .map(i -> weatherRepository.findById(keyPrefix + i))
                .collect(Collectors.toList()));

        StepVerifier.create(weathers)
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
