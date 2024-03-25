package ru.madmax.pet.microcurrency.conslogger.service.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import ru.madmax.pet.microcurrency.common.model.MessageDTO;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ConversionKafkaConsumerTestConfiguration {
    private static final String SECOND_CONSUMER_GROUP = "mc-group-second";
    public static class SecondConsumerBarrierReady {
        private final CountDownLatch latch;

        public SecondConsumerBarrierReady() {
            latch = new CountDownLatch(1);
        }

        public void countDown() {
            latch.countDown();
        }

        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }

    @Bean
    public SecondConsumerBarrierReady secondConsumerBarrierReady() {
        return new SecondConsumerBarrierReady();
    }

    @Bean("secondListenerContainerFactory")
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
        factory.getContainerProperties().setConsumerRebalanceListener(appConsumerRebalanceListenerForTests());
        factory.getContainerProperties().setGroupId(SECOND_CONSUMER_GROUP);
        //Пул потоков (особенно для Concurrency>1), если его контролировать,
        // а не использовать внутренний пул
        var executor = new SimpleAsyncTaskExecutor("mw-consumer-second-");
        executor.setConcurrencyLimit(1);
        var listenerTaskExecutor = new ConcurrentTaskExecutor(executor);
        factory.getContainerProperties().setListenerTaskExecutor(listenerTaskExecutor);
        return factory;
    }

    @Bean
    public ConsumerRebalanceListener appConsumerRebalanceListenerForTests() {
        return new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {}

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                secondConsumerBarrierReady().countDown();
            }
        };
    }

}
