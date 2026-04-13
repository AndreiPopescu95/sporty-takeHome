package com.sportygroup.betting.kafka;

import com.sportygroup.betting.api.dto.EventOutcomeDto;
import com.sportygroup.betting.service.BetSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to the "event-outcomes" topic.
 *
 * WHY @KafkaListener?
 *   Spring's @KafkaListener annotation handles all the polling loop,
 *   deserialization, and thread management for us. The alternative — using
 *   the native KafkaConsumer API — would require significantly more boilerplate.
 *
 * WHY containerFactory = "kafkaListenerContainerFactory"?
 *   Spring Boot auto-configures a default factory from application.properties.
 *   We rely on that default, keeping configuration centralized.
 *
 * WHY pass the full ConsumerRecord instead of just the value?
 *   ConsumerRecord gives us access to metadata: partition, offset, key, headers.
 *   This is useful for logging (tracing which partition a message came from)
 *   and for advanced error handling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventOutcomeConsumer {

    private final BetSettlementService betSettlementService;

    @KafkaListener(
            topics = "${kafka.topic.event-outcomes}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, EventOutcomeDto> eventRecord) {
        log.info("Consumed event outcome from Kafka: topic={}, partition={}, offset={}, key={}",
                eventRecord.topic(), eventRecord.partition(), eventRecord.offset(), eventRecord.key());

        EventOutcomeDto eventOutcome = eventRecord.value();
        log.info("Event outcome payload: eventId={}, eventName={}, winner={}",
                eventOutcome.getEventId(), eventOutcome.getEventName(), eventOutcome.getEventWinnerId());

        // Delegate all business logic to the service layer
        betSettlementService.processEventOutcome(eventOutcome);
    }
}
