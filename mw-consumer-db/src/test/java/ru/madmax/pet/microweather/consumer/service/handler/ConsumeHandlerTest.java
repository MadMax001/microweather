package ru.madmax.pet.microweather.consumer.service.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static ru.madmax.pet.microweather.common.model.MessageType.ERROR;
import static ru.madmax.pet.microweather.common.model.MessageType.WEATHER;

@ExtendWith({MockitoExtension.class})
class ConsumeHandlerTest {
    ConsumeHandler consumeHandler;

    @Mock
    WeatherRepository weatherRepository;

    @Mock
    ErrorRepository errorRepository;

    @Mock
    ModelDomainConverter<String, Weather, WeatherDomain> weatherDomainConverter;

    @Mock
    ModelDomainConverter<String, String, ErrorDomain> errorDomainConverter;

    @Mock
    Hook<MessageDTO> consumerHook;
    @Mock
    Hook<String> successfulCompletionHook;
    @Mock
    Hook<Throwable> errorCompletionHook;


    @Mock
    LogService logService;

    @Mock
    ObjectMapper objectMapper;


    final String WEATHER_KEY = "test_handler_weather_key";
    final String ERROR_KEY = "test_handler_error_key";

    @BeforeEach
    void setUp() {
        consumeHandler = new ConsumeHandler(
                weatherRepository,
                errorRepository,
                weatherDomainConverter,
                errorDomainConverter,
                objectMapper,
                logService,
                consumerHook,
                successfulCompletionHook,
                errorCompletionHook
        );
    }

    @Test
    void handleWeatherType_AndRepositoryAndConverterInvokes_AndCheckLog()
            throws InterruptedException, JsonProcessingException {
        var weatherDomain = TestWeatherDomainBuilder.aWeatherDomain().withId(WEATHER_KEY).build();
        when(weatherDomainConverter.convert(anyString(), any(Weather.class))).thenReturn(weatherDomain);
        when(weatherRepository.save(weatherDomain)).thenAnswer(invocation ->
                Mono.just(weatherDomain).delayElement(Duration.ofMillis(50)));

        var weather = TestWeatherBuilder.aWeather().build();
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(weather);

        var  message = TestMessageDTOBuilder.aMessageDTO()
                .withType(WEATHER)
                .withMessage("{\"now\":1234567890,\"fact\":{\"temp\":10.0,\"windSpeed\":5.3},\"info\":{\"url\":\"www.test.ru\"}}")
                .build();
        CountDownLatch handlerBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            handlerBarrier.countDown();
            return null;
        }).when(successfulCompletionHook).accept(eq(WEATHER_KEY), anyString());

        consumeHandler.accept(WEATHER_KEY, message);

        verify(weatherRepository, times(1)).save(any(WeatherDomain.class));
        verify(errorDomainConverter, never()).convert(anyString(), anyString());
        verify(consumerHook, times(1)).accept(WEATHER_KEY, message);


        var handleSuccess = handlerBarrier.await(1, TimeUnit.SECONDS);
        if (!handleSuccess)
            throw new AppConsumerException(new RuntimeException("Can't handle result"));
        verify(weatherDomainConverter, times(1)).convert(eq(WEATHER_KEY), any(Weather.class));
        verify(errorRepository, never()).save(any(ErrorDomain.class));

        verify(successfulCompletionHook, times(1)).accept(eq(WEATHER_KEY), anyString());
        verify(errorCompletionHook, never()).accept(anyString(), any());
    }

    @Test
    void handleErrorType_AndRepositoryAndConverterInvokes_AndCheckLog() throws InterruptedException {
        var errorDomain = TestErrorDomainBuilder.anErrorDomain().withId(ERROR_KEY).build();
        when(errorDomainConverter.convert(anyString(), anyString())).thenReturn(errorDomain);
        when(errorRepository.save(errorDomain)).thenAnswer(invocation ->
                Mono.just(errorDomain).delayElement(Duration.ofMillis(50)));

        CountDownLatch handlerBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            handlerBarrier.countDown();
            return null;
        }).when(errorCompletionHook).accept(eq(ERROR_KEY), any());

        var  message = TestMessageDTOBuilder.aMessageDTO().withType(ERROR).build();
        consumeHandler.accept(ERROR_KEY, message);

        verify(weatherRepository, never()).save(any(WeatherDomain.class));
        verify(errorDomainConverter, times(1)).convert(anyString(), anyString());
        verify(consumerHook, times(1)).accept(ERROR_KEY, message);

        var handleSuccess = handlerBarrier.await(1, TimeUnit.SECONDS);
        if (!handleSuccess)
            throw new AppConsumerException(new RuntimeException("Can't handle result"));

        verify(weatherDomainConverter, never()).convert(eq(WEATHER_KEY), any(Weather.class));
        verify(errorRepository, times(1)).save(any(ErrorDomain.class));

        verify(successfulCompletionHook, never()).accept(anyString(), anyString());
        verify(errorCompletionHook, times(1)).accept(eq(ERROR_KEY), any());

    }

    @Test
    void handleWeatherType_WithWrongWeatherStructure_ThrowsAppConsumerException_AndCheckNoLog() throws JsonProcessingException {
        Throwable error = new JsonParseException("test-error");
        doThrow(error).when(objectMapper).readValue(anyString(), any(Class.class));


        var  message = TestMessageDTOBuilder.aMessageDTO().withType(WEATHER).build();
        assertThatThrownBy(() -> consumeHandler.accept(WEATHER_KEY, message)).isInstanceOf(AppConsumerException.class);

        verify(successfulCompletionHook, never()).accept(anyString(), anyString());
        verify(errorCompletionHook, never()).accept(anyString(), any());
        verify(errorCompletionHook, never()).accept(eq(WEATHER_KEY), any(JsonParseException.class));

    }
}