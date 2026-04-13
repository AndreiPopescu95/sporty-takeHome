package com.sportygroup.betting.service;

import com.sportygroup.betting.api.dto.BetSettlementDto;
import com.sportygroup.betting.api.dto.EventOutcomeDto;
import com.sportygroup.betting.domain.model.Bet;
import com.sportygroup.betting.domain.repository.BetRepository;
import com.sportygroup.betting.rocketmq.BetSettlementProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration test for BetSettlementService.
 *
 * Uses @SpringBootTest to load the full Spring context with H2 in-memory DB.
 * Kafka and RocketMQ infrastructure are mocked out with @MockBean so no
 * broker is needed to run these tests.
 */
@SpringBootTest
@ActiveProfiles("test")
class BetSettlementServiceTest {

    @Autowired
    private BetSettlementService betSettlementService;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private UserLedger userLedger;

    // Mock Kafka so the full Spring context wires up without a running broker
    @MockBean
    private KafkaTemplate<String, EventOutcomeDto> kafkaTemplate;

    // Mock the RocketMQ producer so we can verify it was called with the right payload
    @MockBean
    private BetSettlementProducer betSettlementProducer;

    @BeforeEach
    void setUp() {
        betRepository.deleteAll();

        betRepository.save(Bet.builder()
                .id("test-bet-1")
                .userId("user-1")
                .eventId("test-event")
                .eventMarketId("market-1")
                .eventWinnerId("team-1")
                .betAmount(new BigDecimal("50.00"))
                .status(Bet.BetStatus.PENDING)
                .build());

        betRepository.save(Bet.builder()
                .id("test-bet-2")
                .userId("user-2")
                .eventId("test-event")
                .eventMarketId("market-1")
                .eventWinnerId("team-2")
                .betAmount(new BigDecimal("100.00"))
                .status(Bet.BetStatus.PENDING)
                .build());
    }

    @Test
    void whenEventOutcomePublished_thenBetStatusesAreUpdatedCorrectly() {
        EventOutcomeDto outcome = EventOutcomeDto.builder()
                .eventId("test-event")
                .eventName("Test Match")
                .eventWinnerId("team-1")
                .build();

        betSettlementService.processEventOutcome(outcome);

        Bet winningBet = betRepository.findById("test-bet-1").orElseThrow();
        assertThat(winningBet.getStatus()).isEqualTo(Bet.BetStatus.WON);

        Bet losingBet = betRepository.findById("test-bet-2").orElseThrow();
        assertThat(losingBet.getStatus()).isEqualTo(Bet.BetStatus.LOST);
    }

    @Test
    void whenEventOutcomePublished_thenRocketMQProducerIsCalledForEachBet() {
        EventOutcomeDto outcome = EventOutcomeDto.builder()
                .eventId("test-event")
                .eventName("Test Match")
                .eventWinnerId("team-1")
                .build();

        betSettlementService.processEventOutcome(outcome);

        // Verify RocketMQ producer was called exactly twice (once per bet)
        ArgumentCaptor<BetSettlementDto> captor = ArgumentCaptor.forClass(BetSettlementDto.class);
        verify(betSettlementProducer, times(2)).send(captor.capture());

        List<BetSettlementDto> sentMessages = captor.getAllValues();

        BetSettlementDto wonMessage = sentMessages.stream()
                .filter(m -> m.getBetId().equals("test-bet-1"))
                .findFirst().orElseThrow();
        assertThat(wonMessage.getOutcome()).isEqualTo("WON");
        assertThat(wonMessage.getActualWinnerId()).isEqualTo("team-1");

        BetSettlementDto lostMessage = sentMessages.stream()
                .filter(m -> m.getBetId().equals("test-bet-2"))
                .findFirst().orElseThrow();
        assertThat(lostMessage.getOutcome()).isEqualTo("LOST");
    }

    @Test
    void whenNoMatchingBets_thenRocketMQProducerIsNeverCalled() {
        EventOutcomeDto outcome = EventOutcomeDto.builder()
                .eventId("unknown-event")
                .eventName("Unknown Match")
                .eventWinnerId("team-x")
                .build();

        betSettlementService.processEventOutcome(outcome);

        verify(betSettlementProducer, never()).send(any());
    }
}
