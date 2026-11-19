package com.codemonk.common.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class KafkaProducerConfigTest {

    private final Map<String, Object> props = KafkaProducerConfig.producerProperties("broker-1:9092,broker-2:9092");

    @Test
    void shouldUseStringKeysAndJsonEventValues() {
        assertEquals(StringSerializer.class, props.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        assertEquals(JsonEventSerializer.class, props.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
    }

    @Test
    void shouldPassTheBootstrapServersThrough() {
        assertEquals("broker-1:9092,broker-2:9092", props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void shouldWaitForAllReplicasAndEnableIdempotence() {
        assertEquals("all", props.get(ProducerConfig.ACKS_CONFIG));
        assertEquals(true, props.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
    }

    @Test
    void shouldBatchBrieflyAndCompress() {
        assertEquals(5, props.get(ProducerConfig.LINGER_MS_CONFIG));
        assertEquals("lz4", props.get(ProducerConfig.COMPRESSION_TYPE_CONFIG));
    }

    @Test
    void shouldBeAcceptedByTheKafkaClientsOwnValidation() {
        ProducerConfig config = assertDoesNotThrow(() -> new ProducerConfig(props));

        assertEquals(JsonEventSerializer.class, config.getClass(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
        assertTrue(config.getBoolean(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
    }

    @Test
    void shouldReturnANewMapOnEveryCall() {
        Map<String, Object> first = KafkaProducerConfig.producerProperties("a:9092");
        first.put(ProducerConfig.ACKS_CONFIG, "0");

        Map<String, Object> second = KafkaProducerConfig.producerProperties("a:9092");

        assertNotEquals(first, second);
        assertEquals("all", second.get(ProducerConfig.ACKS_CONFIG));
    }

    @Test
    void shouldDefaultTheBootstrapServersToLocalhost() {
        assertEquals("localhost:9092", KafkaProducerConfig.DEFAULT_BOOTSTRAP_SERVERS);
    }

    @Test
    void shouldBuildARealProducerWithoutNeedingABroker() {
        try (KafkaProducer<String, Object> producer =
                     new KafkaProducer<>(KafkaProducerConfig.producerProperties("localhost:9092"))) {
            assertNotNull(producer);
        }
    }

    @Test
    void shouldRegisterTheProducerAsALazyBeanThatIsClosedOnShutdown() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(KafkaProducerConfig.class)) {
            BeanDefinition definition = context.getBeanFactory().getBeanDefinition("kafkaProducer");

            assertTrue(definition.isLazyInit());
            assertEquals("close", definition.getDestroyMethodName());
        }
    }
}