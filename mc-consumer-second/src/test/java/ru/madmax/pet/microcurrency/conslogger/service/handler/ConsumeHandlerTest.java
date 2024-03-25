package ru.madmax.pet.microcurrency.conslogger.service.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;
import ru.madmax.pet.microcurrency.common.model.TestConversionBuilder;
import ru.madmax.pet.microcurrency.common.model.TestMessageDTOBuilder;
import ru.madmax.pet.microcurrency.conslogger.exception.AppConsumerException;
import ru.madmax.pet.microcurrency.conslogger.service.LogService;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static ru.madmax.pet.microcurrency.common.model.MessageType.CURRENCY;
import static ru.madmax.pet.microcurrency.common.model.MessageType.ERROR;

@ExtendWith({MockitoExtension.class})
class ConsumeHandlerTest {
    ConsumeHandler consumeHandler;

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


    final String CURRENCY_KEY = "test_handler_currency_key";
    final String ERROR_KEY = "test_handler_error_key";

    @BeforeEach
    void setUp() {
        consumeHandler = new ConsumeHandler(
                logService,
                consumerHook,
                errorCompletionHook,
                successfulCompletionHook
        );
    }

    @Test
    void handleCurrencyType_AndRepositoryAndConverterInvokes_AndCheckLog()
            throws InterruptedException, JsonProcessingException {

        var  message = TestMessageDTOBuilder.aMessageDTO()
                .withType(CURRENCY)
                .withMessage("{\"base\":\"RUB\",\"convert\":\"USD\",\"baseAmount\":10000,\"conversionAmount\":155.8060,\"source\":\"http://www.test.ru\"}")
                .build();
        CountDownLatch handlerBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            handlerBarrier.countDown();
            return null;
        }).when(successfulCompletionHook).accept(eq(CURRENCY_KEY), anyString());

        consumeHandler.accept(CURRENCY_KEY, message);

        verify(consumerHook, times(1)).accept(CURRENCY_KEY, message);

        var handleSuccess = handlerBarrier.await(1, TimeUnit.SECONDS);
        if (!handleSuccess)
            throw new AppConsumerException(new RuntimeException("Can't handle result"));

        verify(successfulCompletionHook, times(1)).accept(eq(CURRENCY_KEY), anyString());
        verify(errorCompletionHook, never()).accept(anyString(), any());
    }

    @Test
    void handleErrorType_AndRepositoryAndConverterInvokes_AndCheckLog() throws InterruptedException {

        CountDownLatch handlerBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            handlerBarrier.countDown();
            return null;
        }).when(errorCompletionHook).accept(eq(ERROR_KEY), any());

        var  message = TestMessageDTOBuilder.aMessageDTO().withType(ERROR).build();
        consumeHandler.accept(ERROR_KEY, message);

        verify(consumerHook, times(1)).accept(ERROR_KEY, message);

        var handleSuccess = handlerBarrier.await(1, TimeUnit.SECONDS);
        if (!handleSuccess)
            throw new AppConsumerException(new RuntimeException("Can't handle result"));

        verify(successfulCompletionHook, never()).accept(anyString(), anyString());
        verify(errorCompletionHook, times(1)).accept(eq(ERROR_KEY), any());

    }

}