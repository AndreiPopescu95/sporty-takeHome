package com.sportygroup.betting.kafka;

import com.sportygroup.betting.api.dto.EventOutcomeDto;
import com.sportygroup.betting.service.BetSettlementService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventOutcomeConsumer.
 *
 * The consumer's only responsibility is to extract the payload from the
 * Kafka record and delegate to BetSettlementService. These tests verify
 * exactly that — nothing more.
 */
@ExtendWith(MockitoExtension.class)
class EventOutcomeConsumerTest {

    @Mock
    private BetSettlementService betSettlementService;

    @InjectMocks
    private EventOutcomeConsumer eventOutcomeConsumer;

    @Test
    void givenKafkaRecord_whenConsume_thenDelegatesToSettlementService() {
        EventOutcomeDto dto = buildDto("event-1", "Chelsea vs Arsenal", "team-1");
        ConsumerRecord<String, EventOutcomeDto> eventRecord =
                new ConsumerRecord<>("event-outcomes", 0, 0L, "event-1", dto);

        eventOutcomeConsumer.consume(eventRecord);

        verify(betSettlementService, times(1)).processEventOutcome(dto);
    }

    @Test
    void givenKafkaRecord_whenConsume_thenPassesCorrectPayloadToService() {
        EventOutcomeDto dto = buildDto("event-42", "Lakers vs Warriors", "team-lakers");
        ConsumerRecord<String, EventOutcomeDto> eventRecord =
                new ConsumerRecord<>("event-outcomes", 2, 15L, "event-42", dto);

        eventOutcomeConsumer.consume(eventRecord);

        ArgumentCaptor<EventOutcomeDto> captor = ArgumentCaptor.forClass(EventOutcomeDto.class);
        verify(betSettlementService).processEventOutcome(captor.capture());

        EventOutcomeDto captured = captor.getValue();
        assertThat(captured.getEventId()).isEqualTo("event-42");
        assertThat(captured.getEventName()).isEqualTo("Lakers vs Warriors");
        assertThat(captured.getEventWinnerId()).isEqualTo("team-lakers");
    }

    @Test
    void givenMultipleRecords_whenConsumedSequentially_thenServiceCalledForEach() {
        EventOutcomeDto dto1 = buildDto("event-1", "Match One", "team-1");
        EventOutcomeDto dto2 = buildDto("event-2", "Match Two", "team-2");

        eventOutcomeConsumer.consume(new ConsumerRecord<>("event-outcomes", 0, 0L, "event-1", dto1));
        eventOutcomeConsumer.consume(new ConsumerRecord<>("event-outcomes", 0, 1L, "event-2", dto2));

        verify(betSettlementService, times(2)).processEventOutcome(any());
    }

    @Test
    void givenServiceThrowsException_whenConsume_thenExceptionPropagates() {
        EventOutcomeDto dto = buildDto("event-1", "Chelsea vs Arsenal", "team-1");
        ConsumerRecord<String, EventOutcomeDto> eventRecord =
                new ConsumerRecord<>("event-outcomes", 0, 0L, "event-1", dto);

        doThrow(new RuntimeException("DB unavailable"))
                .when(betSettlementService).processEventOutcome(any());

        // Exception should propagate so Kafka can handle retry/DLQ behaviour
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> eventOutcomeConsumer.consume(eventRecord));
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
}
