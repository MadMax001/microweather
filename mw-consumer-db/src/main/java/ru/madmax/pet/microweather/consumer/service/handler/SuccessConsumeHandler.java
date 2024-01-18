package ru.madmax.pet.microweather.consumer.service.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
            ).doOnNext(wd -> logService.info(key, "weather data persists"));
        } catch (JsonProcessingException e) {
            throw new AppConsumerException(e);
        }
    }

    private void consumeError(String key, MessageDTO messageDTO) {
        errorRepository.save(
                errorDomainConverter.convert(key, messageDTO.getMessage())
        ).doOnNext(wd -> logService.info(key, "error data persists"));
    }
}
