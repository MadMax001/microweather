package ru.madmax.pet.microweather.consumer.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.madmax.pet.microweather.common.model.MessageDTO;
import ru.madmax.pet.microweather.consumer.repository.ErrorRepository;
import ru.madmax.pet.microweather.consumer.repository.WeatherRepository;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class SuccessConsumeHandler implements BiConsumer<String, MessageDTO> {
    private final WeatherRepository weatherRepository;
    private final ErrorRepository errorRepository;
    
    @Override
    public void accept(String key, MessageDTO messageDTO) {

    }
}
