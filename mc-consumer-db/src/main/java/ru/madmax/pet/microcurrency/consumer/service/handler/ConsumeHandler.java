package ru.madmax.pet.microcurrency.consumer.service.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;
import ru.madmax.pet.microcurrency.consumer.service.stub.SourceCacheStub;
import ru.madmax.pet.microcurrency.common.model.Conversion;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;
import ru.madmax.pet.microcurrency.consumer.exception.AppConsumerException;
import ru.madmax.pet.microcurrency.consumer.exception.RemoteServiceException;
import ru.madmax.pet.microcurrency.consumer.model.ErrorEntity;
import ru.madmax.pet.microcurrency.consumer.model.ConversionEntity;
import ru.madmax.pet.microcurrency.consumer.repository.ErrorRepository;
import ru.madmax.pet.microcurrency.consumer.repository.ConversionRepository;
import ru.madmax.pet.microcurrency.consumer.service.LogService;
import ru.madmax.pet.microcurrency.consumer.service.converter.model.ModelConverter;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class ConsumeHandler implements BiConsumer<String, MessageDTO> {
    private final ConversionRepository conversionRepository;
    private final ErrorRepository errorRepository;
    private final ModelConverter<String, Conversion, ConversionEntity> currencyDomainConverter;
    private final ModelConverter<String, String, ErrorEntity> errorDomainConverter;
    private final ObjectMapper objectMapper;
    private final LogService logService;
    private final Hook<MessageDTO> consumerHook;
    private final Hook<String> successfulCompletionHook;
    private final Hook<Throwable> errorCompletionHook;
    private final SourceCacheStub sourceCache;

    @Override
    public void accept(String key, MessageDTO messageDTO) {
        consumerHook.accept(key, messageDTO);
        switch (messageDTO.getType()) {
            case CURRENCY -> consumeCurrency(key, messageDTO.getMessage());
            case ERROR -> consumeError(key, messageDTO.getMessage());
            default -> wrongMessageType(key, messageDTO.getMessage());
        }
    }

    private void consumeCurrency(String key, String message) {
        try {
            var conversion = objectMapper.readValue(message, Conversion.class);
            var conversionEntity = currencyDomainConverter.convert(key, conversion);
            conversionEntity.setSourceId(sourceCache.getIdBySource(
                    conversion.getSource()
            ));
            conversionRepository.save(conversionEntity)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(wd -> conversionDataPersisting(key, message))
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

    private void conversionDataPersisting(String key, String message) {
        logService.info(key, "Conversion data persists");
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

    private void wrongMessageType (String key, String message) {
        logService.error(key,
                "Wrong message type. " + message);
        Throwable error = new AppConsumerException(new RuntimeException("Wrong message type. " + message));
        errorCompletionHook.accept(key, error);

    }

}
