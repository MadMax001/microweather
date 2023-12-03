package ru.madmax.pet.microweather.producer.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.producer.model.Point;
import ru.madmax.pet.microweather.producer.model.RequestParams;
import ru.madmax.pet.microweather.producer.model.Weather;
import ru.madmax.pet.microweather.producer.model.WeatherBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")
class WeatherFacadeServiceTest {
    final WeatherService weatherService;

    @MockBean
    WeatherRequestService requestService;

    @MockBean
    WeatherProducerService producerService;

    @Test
    void checkRequestAndProduceInvocations() {
        Weather weather = WeatherBuilder.aWeather().build();
        String guid = "test-guid-1";
        RequestParams params = RequestParams.builder().guid(guid).build();

        when(requestService.registerRequest(any(Point.class), params))
                .thenReturn(Mono.just(weather));
        doNothing().when(producerService).produceWeather(guid, weather);

        Point point = Point.builder().build();
        weatherService.requestAndProduce(point, params);

        verify(requestService, times(1)).registerRequest(point, params);
        verify(producerService, times(1)).produceWeather(guid, weather);
    }
}