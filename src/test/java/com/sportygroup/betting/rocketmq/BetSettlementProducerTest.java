package com.sportygroup.betting.rocketmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportygroup.betting.api.dto.BetSettlementDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BetSettlementProducer.
 *
 * The producer's responsibilities:
 *   1. Serialize the BetSettlementDto to JSON (for logging the mock payload)
 *   2. Delegate to BetSettlementConsumer (simulating broker delivery)
 *
 * We mock BetSettlementConsumer to verify the producer calls it with
 * the correct payload, without triggering payout/ledger side effects.
 */
@ExtendWith(MockitoExtension.class)
class BetSettlementProducerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BetSettlementConsumer betSettlementConsumer;

    @InjectMocks
    private BetSettlementProducer betSettlementProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(betSettlementProducer, "topic", "bet-settlements");
    }

    @Test
    void givenSettlementDto_whenSend_thenConsumerIsCalledWithSamePayload() throws Exception {
        BetSettlementDto dto = buildSettlement("bet-001", "user-1", "WON", "50.00");
        when(objectMapper.writeValueAsString(dto)).thenReturn("{\"betId\":\"bet-001\"}");

        betSettlementProducer.send(dto);

        ArgumentCaptor<BetSettlementDto> captor = ArgumentCaptor.forClass(BetSettlementDto.class);
        verify(betSettlementConsumer, times(1)).consume(captor.capture());
        assertThat(captor.getValue().getBetId()).isEqualTo("bet-001");
        assertThat(captor.getValue().getOutcome()).isEqualTo("WON");
    }

    @Test
    void givenSettlementDto_whenSend_thenPayloadIsSerializedToJson() throws Exception {
        BetSettlementDto dto = buildSettlement("bet-001", "user-1", "WON", "50.00");
        when(objectMapper.writeValueAsString(dto)).thenReturn("{\"betId\":\"bet-001\"}");

        betSettlementProducer.send(dto);

        // Verifies ObjectMapper was called — i.e. the payload was serialized for logging
        verify(objectMapper, times(1)).writeValueAsString(dto);
    }

    @Test
    void givenLostBet_whenSend_thenConsumerIsCalledWithLostOutcome() throws Exception {
        BetSettlementDto dto = buildSettlement("bet-002", "user-2", "LOST", "100.00");
        when(objectMapper.writeValueAsString(dto)).thenReturn("{\"betId\":\"bet-002\"}");

        betSettlementProducer.send(dto);

        ArgumentCaptor<BetSettlementDto> captor = ArgumentCaptor.forClass(BetSettlementDto.class);
        verify(betSettlementConsumer).consume(captor.capture());
        assertThat(captor.getValue().getOutcome()).isEqualTo("LOST");
        assertThat(captor.getValue().getUserId()).isEqualTo("user-2");
    }

    @Test
    void givenSerializationFails_whenSend_thenRuntimeExceptionIsThrown() throws Exception {
        BetSettlementDto dto = buildSettlement("bet-001", "user-1", "WON", "50.00");
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("fail") {});

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> betSettlementProducer.send(dto));

        // Consumer should never be called if serialization failed
        verifyNoInteractions(betSettlementConsumer);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BetSettlementDto buildSettlement(String betId, String userId,
                                              String outcome, String amount) {
        return BetSettlementDto.builder()
                .betId(betId)
                .userId(userId)
                .eventId("event-1")
                .eventName("Chelsea vs Arsenal")
                .betWinnerId("team-1")
                .actualWinnerId("team-1")
                .betAmount(new BigDecimal(amount))
                .outcome(outcome)
                .build();
    }
}
