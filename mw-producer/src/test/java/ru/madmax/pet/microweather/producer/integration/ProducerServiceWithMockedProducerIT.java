package ru.madmax.pet.microweather.producer.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;
import ru.madmax.pet.microweather.common.model.*;
import ru.madmax.pet.microweather.producer.configuration.HttpClientConfiguration;
import ru.madmax.pet.microweather.producer.configuration.KafkaConfiguration;
import ru.madmax.pet.microweather.producer.configuration.WeatherRemoteServicesListBuilder;
import ru.madmax.pet.microweather.producer.controller.ExceptionHandlerController;
import ru.madmax.pet.microweather.producer.controller.ProducerControllerV1;
import ru.madmax.pet.microweather.producer.exception.RemoteServiceException;
import ru.madmax.pet.microweather.producer.model.RequestDTO;
import ru.madmax.pet.microweather.producer.model.TestRequestDTOBuilder;
import ru.madmax.pet.microweather.producer.service.*;
import ru.madmax.pet.microweather.producer.service.handlers.ErrorSendingHandler;
import ru.madmax.pet.microweather.producer.service.handlers.SuccessSendingHandler;
import ru.madmax.pet.microweather.producer.service.kafka.WeatherKafkaSenderServiceViaConsumerFactoryConfiguration;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;

@ExtendWith({SpringExtension.class, MockitoExtension.class/*, EmbeddedKafkaExtension.class*/})
@ActiveProfiles("test")
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "app.weather.timeout=1000",
        "app.weather.retry.attempts=1",
        "app.weather.retry.duration=150",
        "app.weather.services[0].host=http://localhost:44445",
        "app.weather.services[0].id=first",
        "app.weather.services[0].path=/test",
        "spring.kafka.properties.isolation.level=read_committed",
        "spring.kafka.client-id=producer-tester",
        "spring.kafka.topic.name=test-simple-topic",
        "spring.kafka.replication.factor=1",
        "spring.kafka.partition.number=1"
})
@ContextConfiguration(classes = {
        ObjectMapper.class,
        KafkaProperties.class,
        WeatherRemoteServicesListBuilder.class,
        HttpClientConfiguration.class,
        KafkaConfiguration.class,
        WeatherKafkaSenderServiceViaConsumerFactoryConfiguration.class,
        Slf4JLogService.class,
        RandomUUIDGeneratorService.class,
        SuccessSendingHandler.class,
        ErrorSendingHandler.class,
        WeatherKafkaSenderService.class,
        ReactRequestService.class,
        WeatherFacadeService.class,
        ProducerControllerV1.class,
        ExceptionHandlerController.class
})
@AutoConfigureWebTestClient
@EmbeddedKafka(
        bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}"
)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProducerServiceWithMockedProducerIT {
    final WebTestClient webTestClient;
    MockWebServer remoteMockServer;
    @MockBean
    LogService logService;
    @SpyBean
    KafkaTemplate<String, MessageDTO> kafkaTemplate;
    String remoteServiceURL;
    final static String SERVICE_LOCAL_PATH = "/api/v1/register";
    @Captor
    ArgumentCaptor<String> stringCaptor;
    BlockingQueue<ConsumerRecord<String, MessageDTO>> records = new LinkedBlockingQueue<>();
    @BeforeEach
    void initialize() throws IOException {
        remoteMockServer = new MockWebServer();
        remoteMockServer.start(44445);
        remoteServiceURL = remoteMockServer.url("/test").toString();
        webTestClient
                .mutate()
                .responseTimeout(Duration.ofMillis(10000));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (remoteMockServer != null)
            remoteMockServer.shutdown();
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            groupId = "IntegrationTestGroup",
            containerFactory = "kafkaListenerContainerFactory")
    private void listen(ConsumerRecord<String, MessageDTO> consumerRecord) throws InterruptedException {
        records.put(consumerRecord);
    }

    @Test
    void registerWeather_happyPass_CheckResponseAndHeaders_AndCheckKafkaQueue_AndCountLogs()
            throws JsonProcessingException, InterruptedException {
        records.clear();
        var objectMapper = new ObjectMapper();

        final RequestDTO request = TestRequestDTOBuilder.aRequestDTO().build();
        final String stringRequest = objectMapper.writeValueAsString(request);

        String responseContent = "{\"now\":1234567890,\"fact\":{\"temp\":10.0,\"windSpeed\":5.3},\"info\":{\"url\":\"www.test.ru\"}}";
        setMockResponseFromServer(500, responseContent);

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringRequest))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HEADER_REQUEST_ERROR_KEY)
                .returnResult(String.class)
                .getResponseBody()
                .log();


        AtomicReference<String> guidReference = new AtomicReference<>();
        StepVerifier.create(receivedResponseEntityContent)
                .expectNextMatches(guid -> {
                    guidReference.set(guid);
                    return guid.length() == 36;
                })
                .expectComplete()
                .verify();

        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(20, TimeUnit.SECONDS);
        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key()).isEqualTo(guidReference.get());
        assertThat(consumerRecord.value().getMessage()).isEqualTo(responseContent);
        assertThat(consumerRecord.value().getType()).isEqualTo(MessageType.WEATHER);
        assertThat(records).isEmpty();

        verify(logService, times(6)).info(anyString(), stringCaptor.capture());
        verify(logService, never()).error(anyString(), anyString());
        stringCaptor.getAllValues().forEach(System.out::println);
    }

    @Test
    void registerWeather_AndRemoteServiceIsAlways503Unavailable_CheckResponseAndHeader_AndCheckKafkaQueue_AndCountLogs()
            throws JsonProcessingException, InterruptedException {
        records.clear();
        var objectMapper = new ObjectMapper();
        final RequestDTO request = TestRequestDTOBuilder.aRequestDTO().build();
        final String stringRequest = objectMapper.writeValueAsString(request);

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringRequest))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HEADER_REQUEST_ERROR_KEY)
                .returnResult(String.class)
                .getResponseBody()
                .log();


        AtomicReference<String> guidReference = new AtomicReference<>();
        StepVerifier.create(receivedResponseEntityContent)
                .expectNextMatches(guid -> {
                    guidReference.set(guid);
                    return guid.length() == 36;
                })
                .expectComplete()
                .verify();

        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(15, TimeUnit.SECONDS);
        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key()).isEqualTo(guidReference.get());
        assertThat(consumerRecord.value().getMessage()).contains(
                "reactor.core.Exceptions$RetryExhaustedException",
                "Retries exhausted: 1/1");
        assertThat(consumerRecord.value().getType()).isEqualTo(MessageType.ERROR);
        assertThat(records).isEmpty();

        verify(logService, times(7)).info(anyString(), stringCaptor.capture());
        verify(logService, times(3)).error(anyString(), stringCaptor.capture());
        stringCaptor.getAllValues().forEach(System.out::println);

    }

    @Test
    void registerWeather_AndRemoteServiceIsOnce503Unavailable_AndRetry_AndThenSuccessfulReturn_CheckResponseAndHeader_AndCheckKafkaQueue_AndCountLogs()
            throws JsonProcessingException, InterruptedException {
        records.clear();
        var objectMapper = new ObjectMapper();
        final RequestDTO request = TestRequestDTOBuilder.aRequestDTO().build();
        final String stringRequest = objectMapper.writeValueAsString(request);

        final Weather weather = TestWeatherBuilder.aWeather().build();
        final String weatherString = objectMapper.writeValueAsString(weather);

        remoteMockServer.enqueue(new MockResponse()
                .setResponseCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody(weatherString));

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringRequest))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HEADER_REQUEST_ERROR_KEY)
                .returnResult(String.class)
                .getResponseBody()
                .log();


        AtomicReference<String> guidReference = new AtomicReference<>();
        StepVerifier.create(receivedResponseEntityContent)
                .expectNextMatches(guid -> {
                    guidReference.set(guid);
                    return guid.length() == 36;
                })
                .expectComplete()
                .verify();

        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(20, TimeUnit.SECONDS);
        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key()).isEqualTo(guidReference.get());
        assertThat(consumerRecord.value().getMessage()).isEqualTo(weatherString);
        assertThat(consumerRecord.value().getType()).isEqualTo(MessageType.WEATHER);
        assertThat(records).isEmpty();

        verify(logService, times(8)).info(anyString(), stringCaptor.capture());
        verify(logService, times(1)).error(anyString(), stringCaptor.capture());
        stringCaptor.getAllValues().forEach(System.out::println);

    }

    @Test
    void registerWeatherWithWrongStructure_Check400ResponseAndHeader_AndCheckEmptyKafkaQueue_AndCountLogs()
            throws  InterruptedException {
        records.clear();
        final String requestStr = "{\"source\":\"first\",\"ipoint\":{\"lat\":51.534986,\"lon\":46.001373}}";

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Point is not set")
                .returnResult(String.class)
                .getResponseBody()
                .log();

        StepVerifier.create(receivedResponseEntityContent)
                .expectComplete()
                .verify();

        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(10, TimeUnit.SECONDS);
        assertThat(consumerRecord).isNull();

        verify(logService, never()).info(anyString(), anyString());
        verify(logService, never()).error(anyString(), anyString());
    }

    @Test
    void registerWeatherWithWrongSource_Check400ResponseAndHeader_AndCheckEmptyKafkaQueue_AndCountLogs()
            throws  InterruptedException {
        records.clear();
        final String requestStr = "{\"source\":\"fast\",\"point\":{\"lat\":51.534986,\"lon\":46.001373}}";

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestStr))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals(HEADER_REQUEST_ERROR_KEY, "Wrong source")
                .returnResult(String.class)
                .getResponseBody()
                .log();

        StepVerifier.create(receivedResponseEntityContent)
                .expectComplete()
                .verify();

        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(10, TimeUnit.SECONDS);
        assertThat(consumerRecord).isNull();

        verify(logService, times(1)).info(anyString(), stringCaptor.capture());
        verify(logService, never()).error(anyString(), anyString());
        stringCaptor.getAllValues().forEach(System.out::println);

    }

    @Test
    void registerWeather_AndThrowsExceptionInBroker_AndCheckFor200Response_AndCheckEmptyKafkaMessage_AndCountLogs()
            throws JsonProcessingException, InterruptedException {
        records.clear();
        var objectMapper = new ObjectMapper();

        Throwable error = new KafkaException("Mock kafka error!");
        doThrow(error).when(kafkaTemplate).send(anyString(), anyString(), any(MessageDTO.class));

        final RequestDTO request = TestRequestDTOBuilder.aRequestDTO().build();
        final String stringRequest = objectMapper.writeValueAsString(request);

        String responseContent = "{\"now\":1234567890,\"fact\":{\"temp\":10.0,\"windSpeed\":5.3},\"info\":{\"url\":\"www.test.ru\"}}";
        setMockResponseFromServer(500, responseContent);

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringRequest))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HEADER_REQUEST_ERROR_KEY)
                .returnResult(String.class)
                .getResponseBody()
                .log();


        StepVerifier.create(receivedResponseEntityContent)
                .expectNextMatches(guid -> guid.length() == 36)
                .expectComplete()
                .verify();

        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(20, TimeUnit.SECONDS);
        assertThat(consumerRecord).isNull();

        verify(logService, times(5)).info(anyString(), stringCaptor.capture());
        verify(logService, times(1)).error(anyString(), stringCaptor.capture());
        stringCaptor.getAllValues().forEach(System.out::println);
    }

    @Test
    void registerWeather_AndRemoteServerReturnWrongStructure_Check200ResponseAndHeaders_AndCheckKafkaQueue_AndCountLogs()
            throws JsonProcessingException, InterruptedException {
        var objectMapper = new ObjectMapper();

        final RequestDTO request = TestRequestDTOBuilder.aRequestDTO().build();
        final String stringRequest = objectMapper.writeValueAsString(request);

        String remoteServiceResponseContent = "__{bad structure}";
//        setMockResponseFromServer(500, remoteServiceResponseContent, "IllegalModelStructureException: " + remoteServiceResponseContent, 500);

        remoteMockServer.enqueue(new MockResponse()
                .addHeader("Content-Type", MediaType.APPLICATION_JSON)
                .addHeader(HEADER_REQUEST_ERROR_KEY, "IllegalModelStructureException: " + remoteServiceResponseContent)
                .setResponseCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringRequest))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HEADER_REQUEST_ERROR_KEY)
                .returnResult(String.class)
                .getResponseBody()
                .log();


        AtomicReference<String> guidReference = new AtomicReference<>();
        StepVerifier.create(receivedResponseEntityContent)
                .expectNextMatches(guid -> {
                    guidReference.set(guid);
                    return guid.length() == 36;
                })
                .expectComplete()
                .verify();

        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(20, TimeUnit.SECONDS);
        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key()).isEqualTo(guidReference.get());
        assertThat(consumerRecord.value().getMessage()).contains(
                RemoteServiceException.class.getCanonicalName(),
                "IllegalModelStructureException",
                remoteServiceResponseContent
        );
        assertThat(consumerRecord.value().getType()).isEqualTo(MessageType.ERROR);
        assertThat(records).isEmpty();

        verify(logService, times(5)).info(anyString(), stringCaptor.capture());
        verify(logService, times(2)).error(anyString(), stringCaptor.capture());
        stringCaptor.getAllValues().forEach(System.out::println);
    }

    @Test
    void registerWeatherConcurrency_happyPass_CheckResponseAndHeaders_AndCheckKafkaQueue()
            throws JsonProcessingException, InterruptedException {
        records.clear();

        var objectMapper = new ObjectMapper();
        int concurrency = 10;
        RequestDTO[] requests = new RequestDTO[concurrency];
        for (int i = 0; i < concurrency; i++)
            requests[i] = TestRequestDTOBuilder.aRequestDTO()
                    .withSource("first")
                    .withPoint(
                            TestPointBuilder
                                    .aPoint()
                                    .withLat((double)i)
                                    .withLon((double)i)
                                    .build())
                    .build();

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                if ("POST".equals(request.getMethod()) &&
                        request.getPath() != null &&
                        request.getPath().startsWith("/test")) {
                    try {
                        var response = new MockResponse();
                        response.addHeader("Content-Type", MediaType.APPLICATION_JSON);
                        var weather = TestWeatherBuilder.aWeather()
                                .withNow(System.currentTimeMillis())
                                .build();

                        String responseContentString = objectMapper.writeValueAsString(weather);
                        return response
                                .setBody(responseContentString)
                                .setBodyDelay(200, TimeUnit.MILLISECONDS);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                }
                return new MockResponse().setResponseCode(404);
            }
        };

        remoteMockServer.setDispatcher(dispatcher);

        List<Callable<String>> tasks = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(concurrency);
        for (int i = 0; i < concurrency; i++) {
            var stringRequest = objectMapper.writeValueAsString(requests[i]);
            tasks.add(new Callable<>() {
                @Override
                public String call() {
                    return webTestClient
                            .post()
                            .uri(SERVICE_LOCAL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(stringRequest))
                            .accept(MediaType.APPLICATION_JSON)
                            .exchange()
                            .expectStatus().isOk()
                            .expectHeader().doesNotExist(HEADER_REQUEST_ERROR_KEY)
                            .returnResult(String.class)
                            .getResponseBody()
                            .blockFirst();
                }
            });
        }
        Set<String> guidSet =
                executorService.invokeAll(tasks, 1, TimeUnit.SECONDS)
                        .stream()
                        .map(stringFuture -> {
                            try {
                                return stringFuture.get();
                            } catch (InterruptedException|ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toSet());
        assertThat(guidSet).hasSize(concurrency);

        List<MessageDTO> sentMessages = new ArrayList<>();
        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(20, TimeUnit.SECONDS);
        while (consumerRecord != null) {
            assertThat(guidSet).contains(consumerRecord.key());
            assertThat(consumerRecord.value().getType()).isEqualTo(MessageType.WEATHER);
            sentMessages.add(consumerRecord.value());
            consumerRecord = records.poll(20, TimeUnit.SECONDS);
        }
        assertThat(sentMessages).hasSize(concurrency);

        verify(logService, times(6 * concurrency)).info(anyString(), stringCaptor.capture());
        verify(logService, never()).error(anyString(), anyString());
        stringCaptor.getAllValues().forEach(System.out::println);


    }

    @Test
    void registerWeather_WithDelayedRemoteResponseMoreThanTimeout_CheckResponseHeaders_AndCheckKafkaQueue()
            throws JsonProcessingException, InterruptedException {
        records.clear();
        var objectMapper = new ObjectMapper();

        final RequestDTO request = TestRequestDTOBuilder.aRequestDTO().build();
        final String stringRequest = objectMapper.writeValueAsString(request);

        String responseContent = "{\"now\":1234567890,\"fact\":{\"temp\":10.0,\"windSpeed\":5.3},\"info\":{\"url\":\"www.test.ru\"}}";
        setMockResponseFromServer(1200, responseContent);

        var receivedResponseEntityContent = webTestClient
                .post()
                .uri(SERVICE_LOCAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(stringRequest))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HEADER_REQUEST_ERROR_KEY)
                .returnResult(String.class)
                .getResponseBody()
                .log();


        AtomicReference<String> guidReference = new AtomicReference<>();
        StepVerifier.create(receivedResponseEntityContent)
                .expectNextMatches(guid -> {
                    guidReference.set(guid);
                    return guid.length() == 36;
                })
                .expectComplete()
                .verify();

        ConsumerRecord<String, MessageDTO> consumerRecord = records.poll(15, TimeUnit.SECONDS);
        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key()).isEqualTo(guidReference.get());
        assertThat(consumerRecord.value().getMessage()).contains(
                "reactor.core.Exceptions$RetryExhaustedException",
                "Retries exhausted: 1/1");
        assertThat(consumerRecord.value().getType()).isEqualTo(MessageType.ERROR);
        assertThat(records).isEmpty();

        verify(logService, times(7)).info(anyString(), stringCaptor.capture());
        verify(logService, times(3)).error(anyString(), stringCaptor.capture());
        stringCaptor.getAllValues().forEach(System.out::println);
    }

    private void setMockResponseFromServer(int timeout, String responseContentString) {
        setMockResponseFromServer(timeout, responseContentString, null, 200);
    }

    private void setMockResponseFromServer(
            int timeout,
            String responseContentString,
            String errorDetailsHeaderValue,
            int responseCode) {
        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                var response = new MockResponse();
                response.addHeader("Content-Type", MediaType.APPLICATION_JSON);
                if (errorDetailsHeaderValue != null) {
                    response.addHeader(HEADER_REQUEST_ERROR_KEY, errorDetailsHeaderValue);
                    response.setResponseCode(responseCode);
                }
                return response
                        .setBody(responseContentString)
                        .setBodyDelay(timeout, TimeUnit.MILLISECONDS);
            }
        };

        remoteMockServer.setDispatcher(dispatcher);

    }
}
