package ru.madmax.pet.microweather.consumer.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.madmax.pet.microweather.common.model.MessageDTO;

import java.util.HashMap;

@Configuration
@EnableKafka
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class WeatherKafkaListenerServiceConfiguration {
    ObjectMapper objectMapper;
    @Value("${spring.kafka.bootstrap-servers}")
    String brokers;

    @Bean
    public ProducerFactory<String, MessageDTO> producerFactory(ObjectMapper mapper) {
        var props = new HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(mapper)
        );
    }

    @Bean
    public KafkaTemplate<String, MessageDTO> kafkaTemplate (
            ProducerFactory<String, MessageDTO> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}
