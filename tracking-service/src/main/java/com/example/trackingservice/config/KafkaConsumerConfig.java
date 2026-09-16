package com.example.trackingservice.config;

import com.example.trackingservice.dto.OrderEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer (and DLQ producer) configuration for the Tracking Service.
 *
 * Retry + DLQ strategy:
 *   - On an exception, the listener retries with a fixed 1s backoff, up to
 *     {@code maxAttempts} (default 3).
 *   - After exhausting retries, the record is published to
 *     {@code order-events.DLT} using {@link DeadLetterPublishingRecoverer}.
 *   - Records that fail permanently (e.g. invalid status transition) are
 *     NOT retried indefinitely - they are routed to the DLQ immediately.
 *
 * Consumer group:
 *   - {@code tracking-service-group}: persistent consumer group so we get
 *     at-least-once delivery even after a Tracking Service restart.
 *   - {@code enable.auto.commit=false}: we commit offsets manually after
 *     successful processing inside the @Transactional boundary.
 *
 * Deserialization safety:
 *   - We wrap the JsonDeserializer in ErrorHandlingDeserializer so that a
 *     malformed payload doesn't crash the listener loop - it lands in the DLQ.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${tracking.kafka.consumer-group:tracking-service-group}")
    private String consumerGroup;

    @Value("${tracking.kafka.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${tracking.kafka.retry.backoff-ms:1000}")
    private long backoffMs;

    @Bean
    public ConsumerFactory<String, OrderEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderEvent.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.trackingservice.dto");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ProducerFactory<String, OrderEvent> dlqProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        props.put(org.springframework.kafka.support.serializer.JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> dlqKafkaTemplate(ProducerFactory<String, OrderEvent> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderEvent> cf,
            KafkaTemplate<String, OrderEvent> dlqKafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // Retry + DLT: retry up to maxAttempts with a fixed backoff, then DLQ.
        FixedBackOff backOff = new FixedBackOff(backoffMs, maxAttempts - 1L);
        org.springframework.kafka.listener.DefaultErrorHandler errorHandler =
                new org.springframework.kafka.listener.DefaultErrorHandler(
                        new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(dlqKafkaTemplate),
                        backOff
                );
        // Invalid transitions are NOT retriable - they will keep failing.
        errorHandler.addNotRetryableExceptions(
                com.example.trackingservice.service.InvalidStatusTransitionException.class,
                IllegalArgumentException.class
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
