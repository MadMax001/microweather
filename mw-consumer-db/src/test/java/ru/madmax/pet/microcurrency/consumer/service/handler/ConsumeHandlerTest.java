package ru.madmax.pet.microcurrency.consumer.service.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.common.model.Conversion;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;
import ru.madmax.pet.microcurrency.common.model.TestConversionBuilder;
import ru.madmax.pet.microcurrency.common.model.TestMessageDTOBuilder;
import ru.madmax.pet.microcurrency.consumer.service.stub.SourceCacheStub;
import ru.madmax.pet.microcurrency.consumer.exception.AppConsumerException;
import ru.madmax.pet.microcurrency.consumer.model.ErrorEntity;
import ru.madmax.pet.microcurrency.consumer.model.TestErrorDomainBuilder;
import ru.madmax.pet.microcurrency.consumer.model.TestConversionEntityBuilder;
import ru.madmax.pet.microcurrency.consumer.model.ConversionEntity;
import ru.madmax.pet.microcurrency.consumer.repository.ErrorRepository;
import ru.madmax.pet.microcurrency.consumer.repository.ConversionRepository;
import ru.madmax.pet.microcurrency.consumer.service.LogService;
import ru.madmax.pet.microcurrency.consumer.service.converter.model.ModelConverter;

import java.time.Duration;
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
    ConversionRepository conversionRepository;

    @Mock
    ErrorRepository errorRepository;

    @Mock
    ModelConverter<String, Conversion, ConversionEntity> conversionEntityConverter;

    @Mock
    ModelConverter<String, String, ErrorEntity> errorEntityConverter;

    @Mock
    Hook<MessageDTO> consumerHook;
    @Mock
    Hook<String> successfulCompletionHook;
    @Mock
    Hook<Throwable> errorCompletionHook;
    @Mock
    SourceCacheStub sourceCache;


    @Mock
    LogService logService;

    @Mock
    ObjectMapper objectMapper;


    final String CURRENCY_KEY = "test_handler_currency_key";
    final String ERROR_KEY = "test_handler_error_key";

    @BeforeEach
    void setUp() {
        consumeHandler = new ConsumeHandler(
                conversionRepository,
                errorRepository,
                conversionEntityConverter,
                errorEntityConverter,
                objectMapper,
                logService,
                consumerHook,
                successfulCompletionHook,
                errorCompletionHook,
                sourceCache
        );
    }

    @Test
    void handleCurrencyType_AndRepositoryAndConverterInvokes_AndCheckLog()
            throws InterruptedException, JsonProcessingException {
        var conversionEntity = TestConversionEntityBuilder.aConversionEntity().withId(CURRENCY_KEY).build();
        when(conversionEntityConverter.convert(anyString(), any(Conversion.class))).thenReturn(conversionEntity);
        when(conversionRepository.save(conversionEntity)).thenAnswer(invocation ->
                Mono.just(conversionEntity).delayElement(Duration.ofMillis(50)));
        when(sourceCache.getIdBySource(anyString())).thenReturn(5L);

        var conversion = TestConversionBuilder.aConversion().build();
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(conversion);

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

        verify(conversionRepository, times(1)).save(any(ConversionEntity.class));
        verify(errorEntityConverter, never()).convert(anyString(), anyString());
        verify(consumerHook, times(1)).accept(CURRENCY_KEY, message);
        verify(sourceCache, times(1)).getIdBySource(conversion.getSource());

        var handleSuccess = handlerBarrier.await(1, TimeUnit.SECONDS);
        if (!handleSuccess)
            throw new AppConsumerException(new RuntimeException("Can't handle result"));
        verify(conversionEntityConverter, times(1)).convert(eq(CURRENCY_KEY), any(Conversion.class));
        verify(errorRepository, never()).save(any(ErrorEntity.class));

        verify(successfulCompletionHook, times(1)).accept(eq(CURRENCY_KEY), anyString());
        verify(errorCompletionHook, never()).accept(anyString(), any());
    }

    @Test
    void handleErrorType_AndRepositoryAndConverterInvokes_AndCheckLog() throws InterruptedException {
        var errorDomain = TestErrorDomainBuilder.anErrorDomain().withId(ERROR_KEY).build();
        when(errorEntityConverter.convert(anyString(), anyString())).thenReturn(errorDomain);
        when(errorRepository.save(errorDomain)).thenAnswer(invocation ->
                Mono.just(errorDomain).delayElement(Duration.ofMillis(50)));

        CountDownLatch handlerBarrier = new CountDownLatch(1);
        doAnswer(inv -> {
            handlerBarrier.countDown();
            return null;
        }).when(errorCompletionHook).accept(eq(ERROR_KEY), any());

        var  message = TestMessageDTOBuilder.aMessageDTO().withType(ERROR).build();
        consumeHandler.accept(ERROR_KEY, message);

        verify(conversionRepository, never()).save(any(ConversionEntity.class));
        verify(errorEntityConverter, times(1)).convert(anyString(), anyString());
        verify(consumerHook, times(1)).accept(ERROR_KEY, message);

        var handleSuccess = handlerBarrier.await(1, TimeUnit.SECONDS);
        if (!handleSuccess)
            throw new AppConsumerException(new RuntimeException("Can't handle result"));

        verify(conversionEntityConverter, never()).convert(eq(CURRENCY_KEY), any(Conversion.class));
        verify(errorRepository, times(1)).save(any(ErrorEntity.class));

        verify(successfulCompletionHook, never()).accept(anyString(), anyString());
        verify(errorCompletionHook, times(1)).accept(eq(ERROR_KEY), any());

    }

    @Test
    void handleCurrencyType_WithWrongCurrencyStructure_ThrowsAppConsumerException_AndCheckNoLog()
            throws JsonProcessingException {
        Throwable error = new JsonParseException("test-error");
        doThrow(error).when(objectMapper).readValue(anyString(), any(Class.class));


        var  message = TestMessageDTOBuilder.aMessageDTO().withType(CURRENCY).build();
        assertThatThrownBy(() -> consumeHandler.accept(CURRENCY_KEY, message)).isInstanceOf(AppConsumerException.class);

        verify(successfulCompletionHook, never()).accept(anyString(), anyString());
        verify(errorCompletionHook, never()).accept(anyString(), any());
        verify(errorCompletionHook, never()).accept(eq(CURRENCY_KEY), any(JsonParseException.class));

    }
}