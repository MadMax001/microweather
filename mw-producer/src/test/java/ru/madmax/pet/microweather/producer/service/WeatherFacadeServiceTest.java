package ru.madmax.pet.microweather.producer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.madmax.pet.microweather.common.model.*;
import ru.madmax.pet.microweather.producer.configuration.WeatherRemoteServicesListBuilder;
import ru.madmax.pet.microweather.producer.exception.AppProducerException;
import ru.madmax.pet.microweather.producer.exception.WrongSourceException;
import ru.madmax.pet.microweather.producer.model.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "app.weather.services[0].host=http://value1.ru",
        "app.weather.services[0].id=first",
        "app.weather.services[0].path=/value2"})
@ContextConfiguration(classes = {WeatherRemoteServicesListBuilder.class, WeatherFacadeService.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")
class WeatherFacadeServiceTest {
    final WeatherService weatherFacadeService;

    @MockBean
    WeatherRequestService requestService;

    @MockBean
    WeatherProducerService producerService;

    @MockBean
    UUIDGeneratorService uuidGeneratorService;

    @MockBean
    LogService logService;

    @SpyBean
    ObjectMapper objectMapper;

    @Captor
    ArgumentCaptor<Point> pointCaptor;

    @Captor
    ArgumentCaptor<String> infoCaptor;

    @Captor
    ArgumentCaptor<String> errorCaptor;
    @Captor
    ArgumentCaptor<RequestParams> requestParamsCaptor;

    @Captor
    ArgumentCaptor<MessageDTO> messageDTOCaptor;

    @Test
    void registerRequest_andCheckForGUIDReturning() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        Weather weather = TestWeatherBuilder.aWeather().build();
        when(requestService.sendRequest(any(), any())).thenReturn(Mono.just(weather));
        doNothing().when(producerService).produceMessage(any(), any());
        doNothing().when(logService).info(any());

        Point point = TestPointBuilder.aPoint().build();
        RequestDTO requestDTO = TestRequestDTOBuilder.aRequestDTO()
                .withPoint(point)
                .build();
        Mono<String> keyMono = weatherFacadeService.registerRequest(requestDTO);
        StepVerifier.create(keyMono)
                .expectNext(guid)
                .expectComplete()
                .verify();
    }


    @Test
    void registerRequest_happyPass_andCheckForServicesCallsAndTheirParams() throws JsonProcessingException {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        Weather weather = TestWeatherBuilder.aWeather().build();
        MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(weather))
                .build();
        when(requestService.sendRequest(any(Point.class), any(RequestParams.class)))
                .thenReturn(Mono.just(weather));
        doNothing().when(producerService).produceMessage(eq(guid), any(MessageDTO.class));

        doNothing().when(logService).info(any(String.class));

        Point point = TestPointBuilder.aPoint().build();
        RequestDTO requestDTO = TestRequestDTOBuilder.aRequestDTO()
                .withPoint(point)
                .build();
        weatherFacadeService.registerRequest(requestDTO).block();

        verify(requestService, times(1)).sendRequest(pointCaptor.capture(), requestParamsCaptor.capture());
        assertThat(pointCaptor.getValue()).isSameAs(point);
        RequestParams requestParamsForVerify = requestParamsCaptor.getValue();
        assertThat(requestParamsForVerify).isNotNull();
        assertThat(requestParamsForVerify.getGuid()).isEqualTo(guid);
        assertThat(requestParamsForVerify.getUrl()).isNotNull();
        assertThat(requestParamsForVerify.getUrl().toString()).contains("http://value1.ru/value2");

        verify(uuidGeneratorService, times(1)).randomGenerate();
        verify(producerService, times(1)).produceMessage(eq(guid), messageDTOCaptor.capture());
        MessageDTO messageDTOForVerify = messageDTOCaptor.getValue();
        assertThat(messageDTOForVerify).isNotNull();
        assertThat(messageDTOForVerify.getType()).isEqualTo(MessageType.WEATHER);
        assertThat(messageDTOForVerify.getMessage()).isEqualTo(messageDTO.getMessage());

        verify(logService, times(2)).info(infoCaptor.capture());
        List<String> infoStringList = infoCaptor.getAllValues();
        assertThat(infoStringList.get(0)).contains(
                guid,
                requestDTO.getSource(),
                requestDTO.getPoint().getLat().toString(),
                requestDTO.getPoint().getLon().toString());
        assertThat(infoStringList.get(1)).contains(guid);
    }

    @Test
    void registerRequest_ErrorPass_andCheckForServicesCallsAndTheirParams() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        Throwable error = new RuntimeException("Test-error");
        when(requestService.sendRequest(any(Point.class), any(RequestParams.class)))
                .thenReturn(Mono.error(error));
        doNothing().when(producerService).produceMessage(eq(guid), any(MessageDTO.class));

        doNothing().when(logService).info(any(String.class));

        Point point = TestPointBuilder.aPoint().build();
        RequestDTO requestDTO = TestRequestDTOBuilder.aRequestDTO()
                .withPoint(point)
                .build();
        weatherFacadeService.registerRequest(requestDTO).block();

        verify(requestService, times(1)).sendRequest(pointCaptor.capture(), requestParamsCaptor.capture());
        verify(uuidGeneratorService, times(1)).randomGenerate();
        verify(producerService, times(1)).produceMessage(eq(guid), messageDTOCaptor.capture());
        MessageDTO messageDTOForVerify = messageDTOCaptor.getValue();
        assertThat(messageDTOForVerify).isNotNull();
        assertThat(messageDTOForVerify.getType()).isEqualTo(MessageType.ERROR);
        assertThat(messageDTOForVerify.getMessage()).contains("Test-error");

        verify(logService, times(1)).error(errorCaptor.capture());
        String errorString = errorCaptor.getValue();
        assertThat(errorString).contains(
                guid,
                "Error response",
                "Test-error",
                "RuntimeException");
    }

    @Test
    void registerRequest_ReturnEmptyMono_andCheckForServicesCallsAndTheirParams() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        when(requestService.sendRequest(any(Point.class), any(RequestParams.class)))
                .thenReturn(Mono.empty());
        doNothing().when(producerService).produceMessage(eq(guid), any(MessageDTO.class));

        doNothing().when(logService).info(any(String.class));

        Point point = TestPointBuilder.aPoint().build();
        RequestDTO requestDTO = TestRequestDTOBuilder.aRequestDTO()
                .withPoint(point)
                .build();
        weatherFacadeService.registerRequest(requestDTO).block();

        verify(requestService, times(1)).sendRequest(pointCaptor.capture(), requestParamsCaptor.capture());
        verify(uuidGeneratorService, times(1)).randomGenerate();
        verify(producerService, times(1)).produceMessage(eq(guid), messageDTOCaptor.capture());
        MessageDTO messageDTOForVerify = messageDTOCaptor.getValue();
        assertThat(messageDTOForVerify).isNotNull();
        assertThat(messageDTOForVerify.getType()).isEqualTo(MessageType.ERROR);
        assertThat(messageDTOForVerify.getMessage()).contains("Empty response");

        verify(logService, times(1)).error(errorCaptor.capture());
        String errorString = errorCaptor.getValue();
        assertThat(errorString).contains(
                guid,
                "Error response",
                "Empty response",
                "AppProducerException");
    }

    @Test
    void registerRequest_AndThrowsAppProducerException_AndCheckReturningValue() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        Weather weather = TestWeatherBuilder.aWeather().build();
        when(requestService.sendRequest(any(), any())).thenReturn(Mono.just(weather));
        RuntimeException appError = new AppProducerException("Test-error");
        doThrow(appError).when(producerService).produceMessage(any(), any());
        doNothing().when(logService).info(any());

        Point point = TestPointBuilder.aPoint().build();
        RequestDTO requestDTO = TestRequestDTOBuilder.aRequestDTO()
                .withPoint(point)
                .build();
        Mono<String> keyMono = weatherFacadeService.registerRequest(requestDTO);
        StepVerifier.create(keyMono)
                .expectNext(guid)
                .expectComplete()
                .verify();

    }

    @Test
    void registerRequest_AndThrowsAppProducerException_AndCheckForServicesCalls() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        Weather weather = TestWeatherBuilder.aWeather().build();
        when(requestService.sendRequest(any(), any())).thenReturn(Mono.just(weather));
        RuntimeException appError = new AppProducerException("Test-error");
        doThrow(appError).when(producerService).produceMessage(any(), any());
        doNothing().when(logService).info(any());

        Point point = TestPointBuilder.aPoint().build();
        RequestDTO requestDTO = TestRequestDTOBuilder.aRequestDTO()
                .withPoint(point)
                .build();
        weatherFacadeService.registerRequest(requestDTO).block();

        verify(requestService, times(1)).sendRequest(pointCaptor.capture(), requestParamsCaptor.capture());
        verify(uuidGeneratorService, times(1)).randomGenerate();
        verify(producerService, times(2)).produceMessage(eq(guid), messageDTOCaptor.capture());
        List<MessageDTO> messageDTOListForVerify = messageDTOCaptor.getAllValues();
        assertThat(messageDTOListForVerify.get(0)).isNotNull();
        assertThat(messageDTOListForVerify.get(0).getType()).isEqualTo(MessageType.WEATHER);
        assertThat(messageDTOListForVerify.get(1)).isNotNull();
        assertThat(messageDTOListForVerify.get(1).getType()).isEqualTo(MessageType.ERROR);
        assertThat(messageDTOListForVerify.get(1).getMessage()).contains("Test-error");

        verify(logService, times(1)).error(errorCaptor.capture());
        String errorString = errorCaptor.getValue();
        assertThat(errorString).contains(
                guid,
                "Error response",
                "Test-error",
                "AppProducerException");
    }

    @Test
    void requestService_WithDelayInResponseFromRemoteServer_AndCheckThatCompleteOperationIsAfterKeyGeneration()
            throws InterruptedException {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        Weather weather = TestWeatherBuilder.aWeather().build();
        when(requestService.sendRequest(any(Point.class), any(RequestParams.class)))
                .thenReturn(Mono.just(weather).delayElement(Duration.ofSeconds(1)));

        doNothing().when(logService).info(any(String.class));
        final AtomicLong facadeMethodCompleteTime = new AtomicLong(0);
        doAnswer((Answer<Void>) invocationOnMock -> {
            facadeMethodCompleteTime.set(System.nanoTime());
            return null;
        }).when(producerService).produceMessage(eq(guid), any(MessageDTO.class));

        Point point = TestPointBuilder.aPoint().build();
        RequestDTO requestDTO = TestRequestDTOBuilder.aRequestDTO()
                .withPoint(point)
                .build();

        weatherFacadeService.registerRequest(requestDTO);
        var returnFacadeMethodTime = System.nanoTime();

        Thread.sleep(1500);
        assertThat((int)((facadeMethodCompleteTime.get() - returnFacadeMethodTime) / 1_000_000_000)).isOne();
    }

    @Test
    void sendRequest_WithLongTimeKeyGeneration_AndCheckForImmediatelyReturn_ButGetKeyValueAfterPause() {
        final String guid = "test-guid-1";
        doAnswer(AdditionalAnswers.answersWithDelay(1000, invocation -> guid))
                .when(uuidGeneratorService).randomGenerate();

        Weather weather = TestWeatherBuilder.aWeather().build();
        when(requestService.sendRequest(any(Point.class), any(RequestParams.class)))
                .thenReturn(Mono.just(weather));

        doNothing().when(logService).info(any(String.class));

        Point point = TestPointBuilder.aPoint().build();
        RequestDTO requestDTO = TestRequestDTOBuilder.aRequestDTO()
                .withPoint(point)
                .build();

        long startTime = System.nanoTime();
        Mono<String> keyMono = weatherFacadeService.registerRequest(requestDTO);
        var returnFacadeMethodTime = System.nanoTime();
        keyMono.block();
        var keyGenerationTime = System.nanoTime();

        assertThat((int)((returnFacadeMethodTime - startTime) / 1_000_000_000)).isZero();
        assertThat((int)((keyGenerationTime - returnFacadeMethodTime) / 1_000_000_000)).isOne();
    }

    @Test
    void sendRequest_WithWrongSource_AndThrowsWrongSourceException() {
        Point point = TestPointBuilder.aPoint().build();
        RequestDTO requestDTO = TestRequestDTOBuilder.aRequestDTO()
                .withPoint(point)
                .withSource("error-source")
                .build();
        when(uuidGeneratorService.randomGenerate()).thenReturn("guid");

        var mono = weatherFacadeService.registerRequest(requestDTO);
        assertThatThrownBy(mono::block).isInstanceOf(WrongSourceException.class);

    }
}