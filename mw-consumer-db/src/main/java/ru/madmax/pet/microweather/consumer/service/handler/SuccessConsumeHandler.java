package ru.madmax.pet.microweather.consumer.service.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;
import ru.madmax.pet.microweather.common.model.MessageDTO;
import ru.madmax.pet.microweather.common.model.Weather;
import ru.madmax.pet.microweather.consumer.exception.AppConsumerException;
import ru.madmax.pet.microweather.consumer.exception.RemoteServiceException;
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
    private final OperationHook<MessageDTO> consumerHook;
    private final OperationHook<String> successfulCompletionHook;
    private final OperationHook<Throwable> errorCompletionHook;

    @Override
    public void accept(String key, MessageDTO messageDTO) {
        consumerHook.accept(key, messageDTO);
        switch (messageDTO.getType()) {
            case WEATHER -> consumeWeather(key, messageDTO.getMessage());
            case ERROR -> consumeError(key, messageDTO.getMessage());
        }
    }

    private void consumeWeather(String key, String message) {
        try {
            weatherRepository.save(
                            weatherDomainConverter.convert(
                                    key, objectMapper.readValue(message, Weather.class))
                    )
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(wd -> weatherDataPersisting(key, message))
                    .doOnError(error -> failedOnPersisting(key, error))
                    .subscribe();


        } catch (JsonProcessingException e) {
            throw new AppConsumerException(e);
        }
    }

    private void consumeError(String key, String message) {
        errorRepository.save(
                errorDomainConverter.convert(key, message)
        )
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(wd -> errorDataPersisting(key, new RemoteServiceException(message)))
                .doOnError(error -> failedOnPersisting(key, error))
                .subscribe();
    }

    private void weatherDataPersisting(String key, String message) {
        logService.info(key, "Weather data persists");
        successfulCompletionHook.accept(key, message);
    }

    private void errorDataPersisting(String key, Throwable error) {
        logService.info(key, "Error data persists");
        errorCompletionHook.accept(key, error);
    }

    private void failedOnPersisting (String key, Throwable error) {
        logService.error(key,
                "Error on persisting: " +
                        (error.getCause() != null ?
                                error.getCause().getMessage() :
                                error.getMessage()));
        errorCompletionHook.accept(key, error);
    }

}
