package com.sportygroup.betting.kafka;

import com.sportygroup.betting.api.dto.EventOutcomeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer responsible for publishing event outcomes to the
 * "event-outcomes" topic.
 *
 * WHY KafkaTemplate?
 *   KafkaTemplate is Spring's high-level abstraction over the native
 *   Kafka Producer API. It handles serialization, connection pooling,
 *   and integrates cleanly with Spring's transaction support.
 *
 * WHY use eventId as the message key?
 *   Kafka partitions messages by key. Using eventId as the key guarantees
 *   that all outcomes for the same event land on the same partition,
 *   preserving ordering for that event. This matters if multiple outcomes
 *   for the same event could be published (e.g., corrections).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventOutcomeProducer {

    private final KafkaTemplate<String, EventOutcomeDto> kafkaTemplate;

    @Value("${kafka.topic.event-outcomes}")
    private String topic;

    public void publish(EventOutcomeDto eventOutcome) {
        log.info("Publishing event outcome to Kafka topic '{}': eventId={}",
                topic, eventOutcome.getEventId());

        CompletableFuture<SendResult<String, EventOutcomeDto>> future =
                kafkaTemplate.send(topic, eventOutcome.getEventId(), eventOutcome);

        // Async callback: log success or failure without blocking the API thread
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event outcome for eventId={}: {}",
                        eventOutcome.getEventId(), ex.getMessage(), ex);
            } else {
                log.info("Successfully published event outcome for eventId={} to partition={} offset={}",
                        eventOutcome.getEventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
