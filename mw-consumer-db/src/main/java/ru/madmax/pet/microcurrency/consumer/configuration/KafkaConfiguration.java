package ru.madmax.pet.microcurrency.consumer.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.JacksonUtils;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;

import java.util.Collection;

import static org.springframework.kafka.support.serializer.JsonDeserializer.TYPE_MAPPINGS;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final ConsumerBarrierReady consumerBarrierReady;
    /*
    // Инициализацию ObjectMapper-а делаем в виде бина
    // и настраиваем здесь спеицичесике правила для преобразования,
    // а затем именно этот бин используем в инициализации ConsumerFactory
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JacksonUtils.enhancedObjectMapper();
    }

    @Bean
    public ConsumerFactory<String, MessageDTO> consumerFactory(
            KafkaProperties kafkaProperties, ObjectMapper mapper) {
        // props создается не пустой мапой, а уже наполенныеми свойствами из application.yml файла
        var props = kafkaProperties.buildConsumerProperties(null);
        // добавляем те свойства, которые точно не будут меняться от запуска к запуску
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//      Свойство задающее десериализатору возможность работать именно с нашим классом
        props.put(TYPE_MAPPINGS, "ru.madmax.pet.microcurrency.common.model.MessageDTO:ru.madmax.pet.microcurrency.common.model.MessageDTO");

        // Размер буфера полученных сообщений, который вовзращается консьюмером
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        // Максимальный временной интервал, в течение которого консьюмер должен выдавать poll.
        // Иначе брокер считает, что консьюмер отвалился
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 3_000);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");



        var kafkaConsumerFactory = new DefaultKafkaConsumerFactory<String, MessageDTO>(props);
        kafkaConsumerFactory.setValueDeserializer(new JsonDeserializer<>(mapper));
        return kafkaConsumerFactory;

    }

    @Bean("listenerContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, MessageDTO>>
    listenerContainerFactory(ConsumerFactory<String, MessageDTO> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MessageDTO>();
        factory.setConsumerFactory(consumerFactory);
        // у нас один микросервис (не монолит), который слушает один топик, поэтому Concurrency = 1
        // а так сколько дистнеров, такой и размер Concurrency
        factory.setConcurrency(1);
        // под капотом листнер у нас делает приодически poll из очереди сообщений,
        // и здесь мы задаем прпметр, который влияет на этот интервал
        factory.getContainerProperties().setIdleBetweenPolls(1_000);
        // poll вызвался, но данных сейчас нет и мы ждем указанное время
        factory.getContainerProperties().setPollTimeout(1_000);
        factory.getContainerProperties().setConsumerRebalanceListener(appConsumerRebalanceListener());
        //Пул потоков (особенно для Concurrency>1), если его контролировать,
        // а не использовать внутренний пул
        var executor = new SimpleAsyncTaskExecutor("mw-consumer-");
        executor.setConcurrencyLimit(1);
        var listenerTaskExecutor = new ConcurrentTaskExecutor(executor);
        factory.getContainerProperties().setListenerTaskExecutor(listenerTaskExecutor);
        return factory;
    }

    @Bean
    public ConsumerRebalanceListener appConsumerRebalanceListener() {
        return new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {}

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                consumerBarrierReady.countDown();
            }
        };
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
