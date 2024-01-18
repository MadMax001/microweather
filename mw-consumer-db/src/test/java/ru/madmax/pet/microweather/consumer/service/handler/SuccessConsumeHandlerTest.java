package ru.madmax.pet.microweather.consumer.service.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.common.model.*;
import ru.madmax.pet.microweather.consumer.exception.AppConsumerException;
import ru.madmax.pet.microweather.consumer.model.ErrorDomain;
import ru.madmax.pet.microweather.consumer.model.TestErrorDomainBuilder;
import ru.madmax.pet.microweather.consumer.model.TestWeatherDomainBuilder;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;
import ru.madmax.pet.microweather.consumer.repository.ErrorRepository;
import ru.madmax.pet.microweather.consumer.repository.WeatherRepository;
import ru.madmax.pet.microweather.consumer.service.LogService;
import ru.madmax.pet.microweather.consumer.service.converter.model.ModelDomainConverter;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static ru.madmax.pet.microweather.common.model.MessageType.ERROR;
import static ru.madmax.pet.microweather.common.model.MessageType.WEATHER;

@ExtendWith({MockitoExtension.class})
class SuccessConsumeHandlerTest {
    SuccessConsumeHandler successConsumeHandler;

    @Mock
    WeatherRepository weatherRepository;

    @Mock
    ErrorRepository errorRepository;

    @Mock
    ModelDomainConverter<String, Weather, WeatherDomain> weatherDomainConverter;

    @Mock
    ModelDomainConverter<String, String, ErrorDomain> errorDomainConverter;

    @Mock
    LogService logService;

    @Mock
    ObjectMapper objectMapper;


    final String WEATHER_KEY = "test_handler_weather_key";
    final String ERROR_KEY = "test_handler_error_key";

    ExecutorService executorService = Executors.newCachedThreadPool();

    @BeforeEach
    void setUp() {
        successConsumeHandler = new SuccessConsumeHandler(
                weatherRepository,
                errorRepository,
                weatherDomainConverter,
                errorDomainConverter,
                objectMapper,
                logService
        );
    }

    @Test
    void handleWeatherType_AndRepositoryAndConverterInvokes_AndCheckLog() throws ExecutionException, InterruptedException, JsonProcessingException {
        var weatherDomain = TestWeatherDomainBuilder.aWeatherDomain().withId(WEATHER_KEY).build();
        when(weatherDomainConverter.convert(anyString(), any(Weather.class))).thenReturn(weatherDomain);
        when(weatherRepository.save(weatherDomain)).thenAnswer(invocation ->
                Mono.just(weatherDomain).delayElement(Duration.ofMillis(50)));

        var weather = TestWeatherBuilder.aWeather().build();
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(weather);

        var  messsage = TestMessageDTOBuilder.aMessageDTO()
                .withType(WEATHER)
                .withMessage("{\"now\":1234567890,\"fact\":{\"temp\":10.0,\"windSpeed\":5.3},\"info\":{\"url\":\"www.test.ru\"}}")
                .build();
        successConsumeHandler.accept(WEATHER_KEY, messsage);

        verify(weatherRepository, times(1)).save(any(WeatherDomain.class));
        verify(errorDomainConverter, never()).convert(anyString(), anyString());

        executorService.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            verify(weatherDomainConverter, times(1)).convert(eq(WEATHER_KEY), any(Weather.class));
            verify(errorRepository, never()).save(any(ErrorDomain.class));
        }).get();
    }

    @Test
    void handleErrorType_AndRepositoryAndConverterInvokes_AndCheckLog() throws ExecutionException, InterruptedException {
        var errorDomain = TestErrorDomainBuilder.anErrorDomain().withId(ERROR_KEY).build();
        when(errorDomainConverter.convert(anyString(), anyString())).thenReturn(errorDomain);
        when(errorRepository.save(errorDomain)).thenAnswer(invocation ->
                Mono.just(errorDomain).delayElement(Duration.ofMillis(50)));


        var  messsage = TestMessageDTOBuilder.aMessageDTO().withType(ERROR).build();
        successConsumeHandler.accept(ERROR_KEY, messsage);

        verify(weatherRepository, never()).save(any(WeatherDomain.class));
        verify(errorDomainConverter, times(1)).convert(anyString(), anyString());

        executorService.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            verify(weatherDomainConverter, never()).convert(eq(WEATHER_KEY), any(Weather.class));
            verify(errorRepository, times(1)).save(any(ErrorDomain.class));
        }).get();

    }

    @Test
    void handleWeatherType_WithWrongWeatherStructure_ThrowsAppConsumerException_AndCheckNoLog() throws JsonProcessingException {
        Throwable error = new JsonParseException("test-error");
        doThrow(error).when(objectMapper).readValue(anyString(), any(Class.class));
        var  messsage = TestMessageDTOBuilder.aMessageDTO().withType(WEATHER).build();
        assertThatThrownBy(() -> successConsumeHandler.accept(WEATHER_KEY, messsage)).isInstanceOf(AppConsumerException.class);


    }
}