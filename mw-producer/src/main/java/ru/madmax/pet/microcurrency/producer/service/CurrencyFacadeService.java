package ru.madmax.pet.microcurrency.producer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.producer.configuration.CurrencyRemoteServicesListBuilder;
import ru.madmax.pet.microcurrency.producer.model.RequestParams;
import ru.madmax.pet.microweather.common.model.MessageDTO;
import ru.madmax.pet.microweather.common.model.MessageType;

import java.util.concurrent.CompletableFuture;

import static ru.madmax.pet.microweather.common.model.MessageType.*;

@Service
@AllArgsConstructor
public class CurrencyFacadeService implements CurrencyService {
    private final UUIDGeneratorService uuidGeneratorService;
    private final CurrencyRequestService requestService;
    private final CurrencyProducerService producerService;
    private final CurrencyRemoteServicesListBuilder servicesBuilder;
    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> registerRequest(ClientRequestX request) {
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
            final String guid = uuidGeneratorService.randomGenerate();
            logService.info(
                    guid,
                    String.format("Register request: %s", request.toString()));
            RequestParams params = buildRequestParams (guid, request);
            var monoWeather = requestService.sendRequest(request, params);
            monoWeather
                    .subscribe(
                            currency -> {
                                logService.info(guid, "Get response");
                                produceMessage(guid, CURRENCY, currency);
                            },
                            error -> {
                                logService.error(
                                        guid,
                                        String.format("Error response: %s:%s",
                                                error.getClass().getName(),
                                                error.getMessage()));
                                produceMessage(guid, ERROR, error);
                            }
                    );
            return guid;
        });
        return Mono.fromFuture(cf);
    }

    private RequestParams buildRequestParams (String guid, ClientRequestX request) {
        return RequestParams.builder()
                .guid(guid)
                .url(servicesBuilder.getURLByKey(request.getSource()))
                .build();
    }

    private void produceMessage(String guid, MessageType type, Object object) {
        producerService.produceMessage(
                guid,
                createMessage(type, object));
    }

    private MessageDTO createMessage(MessageType type, Object object) {
        var message = new MessageDTO();
        message.setType(type);
        if (type == CURRENCY) {
            try {
                message.setMessage(objectMapper.writeValueAsString(object));
            } catch (JsonProcessingException e) {
                logService.error(object.toString(), e);
                message.setType(ERROR);
                message.setMessage(String.format("%s: %s (%s)", e.getClass(), e.getMessage(), object));
            }
        }
        if (type == ERROR) {
            var error = (Throwable)object;
            message.setMessage(String.format("%s: %s", error.getClass(), error.getMessage()));
        }

        return message;
    }
}
