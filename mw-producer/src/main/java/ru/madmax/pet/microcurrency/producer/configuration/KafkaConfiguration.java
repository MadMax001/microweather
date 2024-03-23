package ru.madmax.pet.microcurrency.producer.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.JacksonUtils;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    /*
    // Инициализацию ObjectMapper-а делаем в виде бина
    // и настраиваем здесь спеицичесике правила для преобразования,
    // а затем именно этот бин используем в инициализации ProducerFactory
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JacksonUtils.enhancedObjectMapper();
    }

    @Bean
    public ProducerFactory<String, MessageDTO> producerFactory(
            KafkaProperties kafkaProperties, ObjectMapper mapper
    ) {
        //props создается не пустой мапой, а уже наполенныеми свойствами из application.yml файла
        var props = kafkaProperties.buildProducerProperties(null);
        //добавляем те свойства, которые точно не будут меняться от запуска к запуску
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

    /*
    // Кафка при первом обращении к несуществующему топику должна создавать его, если его нет
    // Однако может такого и не случится, поэтому мы должны такое создание сделать в виде бина
     */
    @Bean
    public NewTopic mainTopic(@Value("${spring.kafka.topic.name}") String sendClientTopic,
                          @Value("${spring.kafka.replication.factor}") Integer replicationFactor,
                          @Value("${spring.kafka.partition.number}") Integer partitionNumber) {
        return new NewTopic(sendClientTopic, partitionNumber, replicationFactor.shortValue());
    }
}
