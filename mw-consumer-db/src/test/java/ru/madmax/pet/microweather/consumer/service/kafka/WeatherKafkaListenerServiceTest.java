package ru.madmax.pet.microweather.consumer.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.madmax.pet.microweather.common.model.MessageDTO;
import ru.madmax.pet.microweather.common.model.TestMessageDTOBuilder;
import ru.madmax.pet.microweather.common.model.TestWeatherBuilder;
import ru.madmax.pet.microweather.consumer.service.LogService;
import ru.madmax.pet.microweather.consumer.service.WeatherListenerService;
import ru.madmax.pet.microweather.consumer.service.handler.SuccessConsumeHandler;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.ExpectedCount.once;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(bootstrapServersProperty = "${spring.kafka.bootstrap-servers}",
        topics = "${spring.kafka.topic.name}"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherKafkaListenerServiceTest {
    @Value("${spring.kafka.topic.name}")
    String testTopic;

    @SpyBean
    LogService logService;

    @SpyBean
    SuccessConsumeHandler successConsumeHandler;

    @Captor
    ArgumentCaptor<String> keyCaptor;

    @Captor
    ArgumentCaptor<MessageDTO> messageCaptor;

    final ObjectMapper objectMapper;
    final KafkaTemplate<String, MessageDTO> kafkaTemplate;
    final WeatherListenerService weatherListenerService;

    @Test
    void sendTestMessage_andConsumeIt() throws JsonProcessingException, ExecutionException, InterruptedException {
        var weather = TestWeatherBuilder.aWeather().build();
        var key = "test-consumer-1";

        var messageDTO = TestMessageDTOBuilder.aMessageDTO()
                .withMessage(objectMapper.writeValueAsString(weather))
                .build();
        System.out.println("Start sending");
        SendResult<String, MessageDTO> stringMessageDTOSendResult = kafkaTemplate.send(testTopic, key, messageDTO).get();
        System.out.println(String.format("Sending is complete: %s",stringMessageDTOSendResult));
        Thread.sleep(5000);

        verify(logService, times(1)).info(any(String.class));
        verify(successConsumeHandler, times(1)).accept(keyCaptor.capture(), messageCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo(key);
        assertThat(messageCaptor.getValue()).isEqualTo(messageDTO);
    }
}