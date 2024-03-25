package ru.madmax.pet.microcurrency.conslogger.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;
import ru.madmax.pet.microcurrency.conslogger.exception.AppConsumerException;
import ru.madmax.pet.microcurrency.conslogger.exception.RemoteServiceException;
import ru.madmax.pet.microcurrency.conslogger.service.LogService;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class ConsumeHandler implements BiConsumer<String, MessageDTO> {
    private final LogService logService;
    private final Hook<MessageDTO> consumerHook;
    private final Hook<Throwable> errorCompletionHook;
    private final Hook<String> successfulCompletionHook;

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

        logService.info(key, message);
        successfulCompletionHook.accept(key, message);
    }

    private void consumeError(String key, String message) {

        logService.info(key, message);
        errorCompletionHook.accept(key, new RemoteServiceException(message));
    }

    private void wrongMessageType (String key, String message) {
        logService.error(key,
                "Wrong message type. " + message);
        Throwable error = new AppConsumerException(new RuntimeException("Wrong message type. " + message));
        errorCompletionHook.accept(key, error);

    }

}
