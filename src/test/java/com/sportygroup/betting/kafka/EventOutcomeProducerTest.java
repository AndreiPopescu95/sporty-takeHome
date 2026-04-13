package com.sportygroup.betting.kafka;

import com.sportygroup.betting.api.dto.EventOutcomeDto;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventOutcomeProducer.
 *
 * WHY @ExtendWith(MockitoExtension.class) and not @SpringBootTest?
 *   This is a pure unit test — we only want to verify that the producer
 *   calls KafkaTemplate with the right topic, key, and value.
 *   No Spring context is needed. MockitoExtension is faster and simpler.
 *
 * WHY ReflectionTestUtils.setField?
 *   The topic name is injected via @Value from application.properties,
 *   which isn't available in a plain unit test. ReflectionTestUtils lets
 *   us set that private field directly without needing a Spring context.
 */
@ExtendWith(MockitoExtension.class)
class EventOutcomeProducerTest {

    @Mock
    private KafkaTemplate<String, EventOutcomeDto> kafkaTemplate;

    @InjectMocks
    private EventOutcomeProducer eventOutcomeProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventOutcomeProducer, "topic", "event-outcomes");
    }

    @Test
    void givenEventOutcome_whenPublish_thenKafkaTemplateCalledWithCorrectTopic() {
        EventOutcomeDto dto = buildDto("event-1", "Chelsea vs Arsenal", "team-1");
        mockSuccessfulSend();

        eventOutcomeProducer.publish(dto);

        verify(kafkaTemplate, times(1)).send(eq("event-outcomes"), any(), any());
    }

    @Test
    void givenEventOutcome_whenPublish_thenEventIdIsUsedAsMessageKey() {
        EventOutcomeDto dto = buildDto("event-42", "Test Match", "team-1");
        mockSuccessfulSend();

        eventOutcomeProducer.publish(dto);

        // Key must be eventId so Kafka routes all outcomes for the same event
        // to the same partition, preserving ordering
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("event-outcomes"), keyCaptor.capture(), any());
        assertThat(keyCaptor.getValue()).isEqualTo("event-42");
    }

    @Test
    void givenEventOutcome_whenPublish_thenFullDtoIsPublishedAsValue() {
        EventOutcomeDto dto = buildDto("event-1", "Chelsea vs Arsenal", "team-1");
        mockSuccessfulSend();

        eventOutcomeProducer.publish(dto);

        ArgumentCaptor<EventOutcomeDto> valueCaptor = ArgumentCaptor.forClass(EventOutcomeDto.class);
        verify(kafkaTemplate).send(eq("event-outcomes"), any(), valueCaptor.capture());

        EventOutcomeDto published = valueCaptor.getValue();
        assertThat(published.getEventId()).isEqualTo("event-1");
        assertThat(published.getEventName()).isEqualTo("Chelsea vs Arsenal");
        assertThat(published.getEventWinnerId()).isEqualTo("team-1");
    }

    @Test
    void givenKafkaSendFails_whenPublish_thenNoExceptionPropagated() {
        EventOutcomeDto dto = buildDto("event-1", "Chelsea vs Arsenal", "team-1");

        CompletableFuture<SendResult<String, EventOutcomeDto>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker unavailable"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);

        // The failure is handled in the async callback — it should log but not throw
        eventOutcomeProducer.publish(dto);

        verify(kafkaTemplate, times(1)).send(eq("event-outcomes"), eq("event-1"), any());

    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private EventOutcomeDto buildDto(String eventId, String eventName, String winnerId) {
        return EventOutcomeDto.builder()
                .eventId(eventId)
                .eventName(eventName)
                .eventWinnerId(winnerId)
                .build();
    }

    private void mockSuccessfulSend() {
        ProducerRecord<String, EventOutcomeDto> eventRecord =
                new ProducerRecord<>("event-outcomes", "key", null);
        RecordMetadata metadata =
                new RecordMetadata(new TopicPartition("event-outcomes", 0), 0, 0, 0, 0, 0);
        SendResult<String, EventOutcomeDto> sendResult = new SendResult<>(eventRecord, metadata);

        CompletableFuture<SendResult<String, EventOutcomeDto>> future = new CompletableFuture<>();
        future.complete(sendResult);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);
    }
}
