package com.rubenmarin.enrollmentservice.config;

import com.rubenmarin.enrollmentservice.event.CourseCreatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaErrorHandlingConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);


    /**
     * Global Kafka listener error handler. Failed records are eventually published to:
     * course-events-dlt using the same partition as the source record.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            @Qualifier("deadLetterKafkaTemplate")
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry
    ) {
        // Custom Micrometer metric
        Counter processingDltCounter = Counter.builder("climbing.kafka.dlt.events")
                .description("Number of Kafka records recovered to the Dead Letter Topic")
                .tag("reason", "processing")
                .register(meterRegistry);

        Counter deserializationDltCounter = Counter.builder("climbing.kafka.dlt.events")
                .description("Number of Kafka records recovered to the Dead Letter Topic")
                .tag("reason", "deserialization")
                .register(meterRegistry);

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + "-dlt",
                                        record.partition()
                                )
                );

        // 2 retries after the initial attempt:
        //
        //   attempt 1
        //       ↓ 1 second
        //   retry 1
        //       ↓ 1 second
        //   retry 2
        //       ↓
        //     DLT

        // There is one very important nuance here. Our poison-pill exception is a DeserializationException.
        // Spring Kafka classifies deserialization errors as fatal by default,
        // because retrying the exact same malformed bytes generally cannot make them valid.
        // Therefore this particular error will skip the FixedBackOff retries and go directly to the recoverer/DLT.

        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        backOff
                );

        errorHandler.setRetryListeners(new RetryListener() {

            @Override
            public void failedDelivery(
                    ConsumerRecord<?, ?> record,
                    Exception exception,
                    int deliveryAttempt
            ) {

                log.error(
                        "Kafka processing failed. topic={}, partition={}, offset={}, attempt={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        deliveryAttempt,
                        exception
                );
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception exception) {

                log.warn(
                        "Kafka record recovered to DLT. topic={}, partition={}, offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset()
                );

                // Custom Micrometer metric
                if (isDeserializationFailure(exception)) {
                    deserializationDltCounter.increment();
                } else {
                    processingDltCounter.increment();
                }
            }

            @Override
            public void recoveryFailed(
                    ConsumerRecord<?, ?> record,
                    Exception original,
                    Exception failure
            ) {

                log.error(
                        "Kafka DLT recovery failed. topic={}, partition={}, offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        failure
                );
            }
        });

        return errorHandler;
    }


    /**
     * KafkaTemplate used to publish failed records to the DLT.
     * It must support:
     * byte[] -> deserialization failures
     * CourseCreatedEvent -> listener / processing failures
     */
    @Bean
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate(KafkaProperties kafkaProperties) {

        JacksonJsonSerializer<Object> jsonSerializer = new JacksonJsonSerializer<>();

        // Do not expose Java class names in Kafka headers.
        jsonSerializer.setAddTypeInfo(false);

        Map<Class<?>, Serializer<?>> serializers = new LinkedHashMap<>();
        serializers.put(byte[].class, new ByteArraySerializer());
        serializers.put(CourseCreatedEvent.class, jsonSerializer);

        DelegatingByTypeSerializer valueSerializer = new DelegatingByTypeSerializer(serializers);

        DefaultKafkaProducerFactory<String, Object> producerFactory =
                new DefaultKafkaProducerFactory<>(
                        kafkaProperties.buildProducerProperties(),
                        new StringSerializer(),
                        valueSerializer
                );

        return new KafkaTemplate<>(producerFactory);
    }

    private boolean isDeserializationFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof DeserializationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}