package com.resto.saas.loyalty.consumer;

import com.resto.saas.loyalty.event.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> kafkaListenerContainerFactory() {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:19092,localhost:19093,localhost:19094");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "loyalty-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.resto.saas.order.event.OrderCreatedEvent", OrderCreatedEvent.class);
        typeMapper.setIdClassMapping(idClassMapping);

        JsonDeserializer<OrderCreatedEvent> deserializer = new JsonDeserializer<>(OrderCreatedEvent.class);
        deserializer.setTypeMapper(typeMapper);
        deserializer.addTrustedPackages("com.resto.saas.loyalty.event");
        deserializer.setUseTypeHeaders(true);

        DefaultKafkaConsumerFactory<String, OrderCreatedEvent> factory =
                new DefaultKafkaConsumerFactory<>(props,
                        new StringDeserializer(),
                        deserializer);

        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(factory);
        return containerFactory;
    }
}
