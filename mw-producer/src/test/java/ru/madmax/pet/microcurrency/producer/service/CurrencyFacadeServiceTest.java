package ru.madmax.pet.microcurrency.producer.service;

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
import ru.madmax.pet.microcurrency.producer.configuration.CurrencyRemoteServicesListBuilder;
import ru.madmax.pet.microcurrency.producer.exception.AppProducerException;
import ru.madmax.pet.microcurrency.producer.exception.WrongSourceException;
import ru.madmax.pet.microcurrency.producer.model.*;
import ru.madmax.pet.microweather.common.model.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "app.services[0].host=http://value1.ru",
        "app.services[0].id=first",
        "app.services[0].path=/value2"})
@ContextConfiguration(classes = {CurrencyRemoteServicesListBuilder.class, CurrencyFacadeService.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")
class CurrencyFacadeServiceTest {
    final CurrencyService currencyFacadeService;

    @MockBean
    CurrencyRequestService requestService;

    @MockBean
    CurrencyProducerService producerService;

    @MockBean
    UUIDGeneratorService uuidGeneratorService;

    @MockBean
    LogService logService;

    @SpyBean
    ObjectMapper objectMapper;

    @Captor
    ArgumentCaptor<ServiceRequest> requestCaptor;

    @Captor
    ArgumentCaptor<String> infoCaptor;

    @Captor
    ArgumentCaptor<String> errorCaptor;

    @Captor
    ArgumentCaptor<String> keyCaptor;
    @Captor
    ArgumentCaptor<RequestParams> requestParamsCaptor;

    @Captor
    ArgumentCaptor<MessageDTO> messageDTOCaptor;

    @Test
    void registerRequest_andCheckForGUIDReturning() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        var response = TestResponseBuilder.aResponse().build();
        when(requestService.sendRequest(any(), any())).thenReturn(Mono.just(response));

        doNothing().when(producerService).produceMessage(any(), any());
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        ClientRequestX request = TestCurrencyRequestXBuilder.aRequest().build();

        Mono<String> keyMono = currencyFacadeService.registerRequest(request);
        StepVerifier.create(keyMono)
                .expectNext(guid)
                .expectComplete()
                .verify();
    }


    @Test
    void registerRequest_happyPass_andCheckForServicesCallsAndTheirParams() throws JsonProcessingException {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        var response = TestResponseBuilder.aResponse().build();
        MessageDTO messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(response))
                .build();

        when(requestService.sendRequest(any(ClientRequestX.class), any(RequestParams.class)))
                .thenReturn(Mono.just(response));
        doNothing().when(producerService).produceMessage(eq(guid), any(MessageDTO.class));

        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        ClientRequestX request = TestCurrencyRequestXBuilder.aRequest().build();

        currencyFacadeService.registerRequest(request).block();

        verify(requestService, times(1))
                .sendRequest(requestCaptor.capture(), requestParamsCaptor.capture());

        assertThat(requestCaptor.getValue()).isSameAs(request);
        RequestParams requestParamsForVerify = requestParamsCaptor.getValue();
        assertThat(requestParamsForVerify).isNotNull();
        assertThat(requestParamsForVerify.getGuid()).isEqualTo(guid);
        assertThat(requestParamsForVerify.getUrl()).isNotNull();
        assertThat(requestParamsForVerify.getUrl().toString()).contains("http://value1.ru/value2");

        verify(uuidGeneratorService, times(1)).randomGenerate();
        verify(producerService, times(1)).produceMessage(eq(guid), messageDTOCaptor.capture());
        MessageDTO messageDTOForVerify = messageDTOCaptor.getValue();
        assertThat(messageDTOForVerify).isNotNull();
        assertThat(messageDTOForVerify.getType()).isEqualTo(MessageType.CURRENCY);
        assertThat(messageDTOForVerify.getMessage()).isEqualTo(messageDTO.getMessage());

        verify(logService, times(2)).info(keyCaptor.capture(), infoCaptor.capture());
        List<String> infoStringList = infoCaptor.getAllValues();
        List<String> keyValues = keyCaptor.getAllValues();
        assertThat(infoStringList.get(0)).contains(
                request.getSource(),
                request.getBaseCurrency().name(),
                request.getConvertCurrency().name(),
                request.getBaseAmount().toPlainString());
        assertThat(keyValues).isNotEmpty().allMatch(key -> key.equals(guid));

        verify(logService, never()).error(anyString(), anyString());
    }

    @Test
    void registerRequest_ErrorPass_andCheckForServicesCallsAndTheirParams() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        Throwable error = new RuntimeException("Test-error");
        when(requestService.sendRequest(any(ServiceRequest.class), any(RequestParams.class)))
                .thenReturn(Mono.error(error));

        doNothing().when(producerService).produceMessage(eq(guid), any(MessageDTO.class));

        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        ClientRequestX request = TestCurrencyRequestXBuilder.aRequest().build();
        currencyFacadeService.registerRequest(request).block();

        verify(requestService, times(1)).sendRequest(requestCaptor.capture(), requestParamsCaptor.capture());
        verify(uuidGeneratorService, times(1)).randomGenerate();
        verify(producerService, times(1)).produceMessage(eq(guid), messageDTOCaptor.capture());
        MessageDTO messageDTOForVerify = messageDTOCaptor.getValue();
        assertThat(messageDTOForVerify).isNotNull();
        assertThat(messageDTOForVerify.getType()).isEqualTo(MessageType.ERROR);
        assertThat(messageDTOForVerify.getMessage()).contains("Test-error");

        verify(logService, times(1)).error(keyCaptor.capture(), errorCaptor.capture());
        String errorString = errorCaptor.getValue();
        String keyValue = keyCaptor.getValue();
        assertThat(errorString).contains(
                "Error response",
                "Test-error",
                "RuntimeException");
        assertThat(keyValue).isEqualTo(guid);

        verify(logService, times(1)).info(anyString(), anyString());
    }


    @Test
    void registerRequest_AndThrowsAppProducerException_AndCheckReturningValue() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        var response = TestResponseBuilder.aResponse().build();
        when(requestService.sendRequest(any(), any())).thenReturn(Mono.just(response));

        RuntimeException appError = new AppProducerException("Test-error");
        doThrow(appError).when(producerService).produceMessage(any(), any());
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        ClientRequestX request = TestCurrencyRequestXBuilder.aRequest().build();
        Mono<String> keyMono = currencyFacadeService.registerRequest(request);

        StepVerifier.create(keyMono)
                .expectNext(guid)
                .expectComplete()
                .verify();

    }

    @Test
    void registerRequest_AndThrowsAppProducerException_AndCheckForServicesCalls() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        var response = TestResponseBuilder.aResponse().build();
        when(requestService.sendRequest(any(), any())).thenReturn(Mono.just(response));

        RuntimeException appError = new AppProducerException("Test-error");
        doThrow(appError).when(producerService).produceMessage(any(), any());
        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        ClientRequestX request = TestCurrencyRequestXBuilder.aRequest().build();
        currencyFacadeService.registerRequest(request).block();

        verify(requestService, times(1)).sendRequest(requestCaptor.capture(), requestParamsCaptor.capture());
        verify(uuidGeneratorService, times(1)).randomGenerate();
        verify(producerService, times(2)).produceMessage(eq(guid), messageDTOCaptor.capture());
        List<MessageDTO> messageDTOListForVerify = messageDTOCaptor.getAllValues();
        assertThat(messageDTOListForVerify.get(0)).isNotNull();
        assertThat(messageDTOListForVerify.get(0).getType()).isEqualTo(MessageType.CURRENCY);
        assertThat(messageDTOListForVerify.get(1)).isNotNull();
        assertThat(messageDTOListForVerify.get(1).getType()).isEqualTo(MessageType.ERROR);
        assertThat(messageDTOListForVerify.get(1).getMessage()).contains("Test-error");

        verify(logService, times(1)).error(keyCaptor.capture(), errorCaptor.capture());
        String errorString = errorCaptor.getValue();
        String keyValue = keyCaptor.getValue();
        assertThat(errorString).contains(
                "Error response",
                "Test-error",
                "AppProducerException");
        assertThat(keyValue).isEqualTo(guid);

        verify(logService, times(2)).info(anyString(), anyString());
    }

    @Test
    void requestService_WithDelayInResponseFromRemoteServer_AndCheckThatCompleteOperationIsAfterKeyGeneration() {
        final String guid = "test-guid-1";
        when(uuidGeneratorService.randomGenerate()).thenReturn(guid);

        var response = TestResponseBuilder.aResponse().build();
        when(requestService.sendRequest(any(ServiceRequest.class), any(RequestParams.class)))
                .thenReturn(Mono.just(response).delayElement(Duration.ofSeconds(1)));

        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        final AtomicLong facadeMethodCompleteTime = new AtomicLong(0);
        doAnswer((Answer<Void>) invocationOnMock -> {
            facadeMethodCompleteTime.set(System.nanoTime());
            return null;
        }).when(producerService).produceMessage(eq(guid), any(MessageDTO.class));

        ClientRequestX request = TestCurrencyRequestXBuilder.aRequest().build();
        currencyFacadeService.registerRequest(request);

        var returnFacadeMethodTime = System.nanoTime();

        await().atMost(1500, TimeUnit.MILLISECONDS)
                .until(() ->
                        ((int)((facadeMethodCompleteTime.get() - returnFacadeMethodTime) / 1_000_000_000) != 1));
    }

    @Test
    void sendRequest_WithLongTimeKeyGeneration_AndCheckForImmediatelyReturn_ButGetKeyValueAfterPause() {
        final String guid = "test-guid-1";
        doAnswer(AdditionalAnswers.answersWithDelay(1000, invocation -> guid))
                .when(uuidGeneratorService).randomGenerate();

        var response = TestResponseBuilder.aResponse().build();
        when(requestService.sendRequest(any(ServiceRequest.class), any(RequestParams.class)))
                .thenReturn(Mono.just(response));

        doNothing().when(logService).info(anyString(), anyString());
        doNothing().when(logService).error(anyString(), anyString());

        ClientRequestX request = TestCurrencyRequestXBuilder.aRequest().build();

        long startTime = System.nanoTime();
        Mono<String> keyMono = currencyFacadeService.registerRequest(request);
        var returnFacadeMethodTime = System.nanoTime();
        keyMono.block();
        var keyGenerationTime = System.nanoTime();

        assertThat((int)((returnFacadeMethodTime - startTime) / 1_000_000_000)).isZero();
        assertThat((int)((keyGenerationTime - returnFacadeMethodTime) / 1_000_000_000)).isOne();
    }

    @Test
    void sendRequest_WithWrongSource_AndThrowsWrongSourceException() {
        ClientRequestX request = TestCurrencyRequestXBuilder.aRequest().withSource("wrong-source").build();
        when(uuidGeneratorService.randomGenerate()).thenReturn("guid");

        var mono = currencyFacadeService.registerRequest(request);
        assertThatThrownBy(mono::block).isInstanceOf(WrongSourceException.class);

    }
}