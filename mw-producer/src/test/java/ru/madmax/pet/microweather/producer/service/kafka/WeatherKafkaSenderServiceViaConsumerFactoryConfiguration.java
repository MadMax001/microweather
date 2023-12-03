package ru.madmax.pet.microweather.producer.service.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.madmax.pet.microweather.producer.model.Weather;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
class WeatherKafkaSenderServiceViaConsumerFactoryConfiguration {
    @Value("${spring.kafka.bootstrap-servers}")
    String brokers;
    @Value("${spring.kafka.replication.factor}")
    Integer replicationFactor;
    @Value("${spring.kafka.partition.number}")
    Integer partitionNumber;
    @Value("${spring.kafka.topic.name}")
    String sendClientTopic;
    @Bean
    public ConsumerFactory<String, Weather> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "producer-tester");
//        props.put("allow.auto.create.topics", false);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);


/*
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_FUNCTION, FailedWeatherProvider.class);

        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "ru.madmax.pet.microweather.producer.model.Weather");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "ru.madmax.pet.microweather.producer.model");
*/

        //props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        //props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        //props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
       // props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        //props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, );


/*
        JsonDeserializer<Weather> deserializer = new JsonDeserializer<>(Weather.class, false);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);
*/

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(Weather.class, false)
        );
    }
    @Bean
    KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Weather>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Weather> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);
        return factory;
    }

/*    @Bean
    public NewTopic testTopic() {
        return TopicBuilder.name(sendClientTopic)
                .partitions(partitionNumber)
                .replicas(replicationFactor)
                .build();
    }*/
}
