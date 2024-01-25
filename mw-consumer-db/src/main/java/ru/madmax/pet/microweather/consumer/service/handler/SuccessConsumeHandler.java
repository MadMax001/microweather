package ru.madmax.pet.microweather.consumer.service.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;
import ru.madmax.pet.microweather.common.model.MessageDTO;
import ru.madmax.pet.microweather.common.model.Weather;
import ru.madmax.pet.microweather.consumer.exception.AppConsumerException;
import ru.madmax.pet.microweather.consumer.model.ErrorDomain;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;
import ru.madmax.pet.microweather.consumer.repository.ErrorRepository;
import ru.madmax.pet.microweather.consumer.repository.WeatherRepository;
import ru.madmax.pet.microweather.consumer.service.LogService;
import ru.madmax.pet.microweather.consumer.service.converter.model.ModelDomainConverter;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class SuccessConsumeHandler implements BiConsumer<String, MessageDTO> {
    private final WeatherRepository weatherRepository;
    private final ErrorRepository errorRepository;
    private final ModelDomainConverter<String, Weather, WeatherDomain> weatherDomainConverter;
    private final ModelDomainConverter<String, String, ErrorDomain> errorDomainConverter;
    private final ObjectMapper objectMapper;
    private final LogService logService;

    @Override
    public void accept(String key, MessageDTO messageDTO) {
        switch (messageDTO.getType()) {
            case WEATHER -> consumeWeather(key, messageDTO);
            case ERROR -> consumeError(key, messageDTO);
        }
    }

    private void consumeWeather(String key, MessageDTO messageDTO) {
        try {
            weatherRepository.save(
                            weatherDomainConverter.convert(
                                    key, objectMapper.readValue(messageDTO.getMessage(), Weather.class))
                    )
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(wd -> successfulWeatherPersisting(key))
                    .doOnError(error -> failedWeatherPersisting(key, error))
                    .subscribe();


        } catch (JsonProcessingException e) {
            throw new AppConsumerException(e);
        }
    }

    private void consumeError(String key, MessageDTO messageDTO) {
        errorRepository.save(
                errorDomainConverter.convert(key, messageDTO.getMessage())
        )
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(wd -> successfulErrorPersisting(key))
                .doOnError(error -> failedErrorPersisting(key, error))
                .subscribe();
    }

    private void successfulWeatherPersisting(String key) {
        logService.info(key, "Weather data persists");
    }

    private void failedWeatherPersisting (String key, Throwable error) {
        logService.error(key,
                "Error on persisting: " +
                        (error.getCause() != null ?
                                error.getCause().getMessage() :
                                error.getMessage()));
    }

    private void successfulErrorPersisting(String key) {
        logService.info(key, "Error data persists");
    }

    private void failedErrorPersisting(String key, Throwable error) {
        logService.error(key,
                "Error on persisting: " +
                        (error.getCause() != null ?
                                error.getCause().getMessage() :
                                error.getMessage()));
    }
}
