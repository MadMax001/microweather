package ru.madmax.pet.microweather.producer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.common.model.*;
import ru.madmax.pet.microweather.producer.model.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
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
    ArgumentCaptor<Throwable> errorCaptor;
    @Captor
    ArgumentCaptor<RequestParams> requestParamsCaptor;

    @Captor
    ArgumentCaptor<MessageDTO> messageDTOCaptor;

    @Test
    void registerRequest_happyPass() throws InterruptedException, JsonProcessingException {
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
        String key = weatherFacadeService.registerRequest(requestDTO);
        Thread.sleep(100);

        verify(requestService, times(1)).sendRequest(pointCaptor.capture(), requestParamsCaptor.capture());
        assertThat(key).isEqualTo(guid);
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

        verify(logService, times(1)).info(infoCaptor.capture());
        String infoString = infoCaptor.getValue();
        assertThat(infoString).contains(guid);
    }
}