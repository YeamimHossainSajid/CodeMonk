package com.codemonk.common.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Shared Kafka producer settings for every CodeMonk service.
 *
 * <p>{@link #producerProperties(String)} returns the settings as a plain map, so a service that uses
 * Spring Kafka can hand it to its own producer factory, and a service that uses the Kafka client
 * directly can pass it to {@link KafkaProducer}. Keys are plain strings and values are written as JSON
 * by {@link JsonEventSerializer}, the same serializer that defines the event wire format.
 *
 * <p>The settings favour not losing events: every replica must acknowledge a send ({@code acks=all})
 * and the producer is idempotent, so a retry cannot create a duplicate. Small batches are held for
 * 5 ms and compressed with lz4 to cut network use.
 *
 * <p>The {@link KafkaProducer} bean reads {@code spring.kafka.bootstrap-servers} (the key the services
 * already use) and falls back to {@value #DEFAULT_BOOTSTRAP_SERVERS}. It is lazy, so an application that
 * imports this class but never sends an event does not open a producer, and it is closed when the
 * application context shuts down.
 */
@Configuration
public class KafkaProducerConfig {

    public static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";

    /**
     * Builds the producer settings for the given brokers.
     *
     * @param bootstrapServers comma separated list of {@code host:port} pairs
     * @return a new, modifiable map on every call
     */
    public static Map<String, Object> producerProperties(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonEventSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        return props;
    }

    @Bean(destroyMethod = "close")
    @Lazy
    public KafkaProducer<String, Object> kafkaProducer(
            @Value("${spring.kafka.bootstrap-servers:" + DEFAULT_BOOTSTRAP_SERVERS + "}") String bootstrapServers) {
        return new KafkaProducer<>(producerProperties(bootstrapServers));
    }
}