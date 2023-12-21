package ru.madmax.pet.microweather.producer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.common.model.MessageDTO;
import ru.madmax.pet.microweather.common.model.MessageType;
import ru.madmax.pet.microweather.producer.configuration.WeatherRemoteServicesListBuilder;
import ru.madmax.pet.microweather.producer.exception.AppProducerException;
import ru.madmax.pet.microweather.producer.model.*;

import java.util.concurrent.CompletableFuture;

import static ru.madmax.pet.microweather.common.model.MessageType.ERROR;
import static ru.madmax.pet.microweather.common.model.MessageType.WEATHER;

@Service
@AllArgsConstructor
public class WeatherFacadeService implements WeatherService {
    private final UUIDGeneratorService uuidGeneratorService;
    private final WeatherRequestService requestService;
    private final WeatherProducerService producerService;
    private final WeatherRemoteServicesListBuilder servicesBuilder;
    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> registerRequest(RequestDTO request) {
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
            final String guid = uuidGeneratorService.randomGenerate();
            RequestParams params = RequestParams.builder()
                    .guid(guid)
                    .url(servicesBuilder.getURLByKey(request.getSource()))
                    .build();
            var monoWeather = requestService.sendRequest(request.getPoint(), params);
            monoWeather.subscribe(
                    weather -> {
                        logService.info(String.format("Receive response for guid %s",
                                guid));
                        try {
                            producerService.produceMessage(
                                    guid,
                                    createMessage(WEATHER, weather));
                        } catch (JsonProcessingException e) {
                            throw new AppProducerException(e);
                        }
                    },
                    error -> {
                        logService.error(String.format("Error response for guid %s: %s:%s",
                                guid,
                                error.getClass().getName(),
                                error.getMessage()));
                        try {
                            producerService.produceMessage(
                                    guid,
                                    createMessage(ERROR, error));
                        } catch (JsonProcessingException e) {
                            throw new AppProducerException(e);
                        }
                    }
            );
            return guid;
        });
        return Mono.fromFuture(cf);
    }

    private MessageDTO createMessage(MessageType type, Object object) throws JsonProcessingException {
        var message = new MessageDTO();
        message.setType(type);
        message.setMessage(objectMapper.writeValueAsString(object));
        return message;
    }
}
