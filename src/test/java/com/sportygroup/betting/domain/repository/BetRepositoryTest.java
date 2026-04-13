package com.sportygroup.betting.domain.repository;

import com.sportygroup.betting.domain.model.Bet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository layer tests using @DataJpaTest.
 *
 * WHY @DataJpaTest?
 *   Loads only the JPA layer (entities, repositories, H2).
 *   No web layer, no Kafka, no services. This is the fastest way
 *   to test that our custom query method works correctly.
 */
@DataJpaTest
class BetRepositoryTest {

    @Autowired
    private BetRepository betRepository;

    @BeforeEach
    void setUp() {
        betRepository.deleteAll();

        betRepository.save(buildBet("bet-001", "user-1", "event-1", "team-1", Bet.BetStatus.PENDING));
        betRepository.save(buildBet("bet-002", "user-2", "event-1", "team-2", Bet.BetStatus.PENDING));
        betRepository.save(buildBet("bet-003", "user-3", "event-1", "team-1", Bet.BetStatus.WON));
        betRepository.save(buildBet("bet-004", "user-1", "event-2", "team-lakers", Bet.BetStatus.PENDING));
    }

    @Test
    void givenPendingBetsForEvent_whenFindByEventIdAndStatusPending_thenReturnsOnlyPending() {
        List<Bet> result = betRepository.findByEventIdAndStatus("event-1", Bet.BetStatus.PENDING);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Bet::getId)
                .containsExactlyInAnyOrder("bet-001", "bet-002");
    }

    @Test
    void givenSettledBet_whenFindByEventIdAndStatusPending_thenSettledBetIsExcluded() {
        // bet-003 is WON — should not appear in PENDING query
        List<Bet> result = betRepository.findByEventIdAndStatus("event-1", Bet.BetStatus.PENDING);

        assertThat(result).extracting(Bet::getId)
                .doesNotContain("bet-003");
    }

    @Test
    void givenBetsForDifferentEvents_whenFindByEventId_thenOnlyMatchingEventReturned() {
        List<Bet> result = betRepository.findByEventIdAndStatus("event-2", Bet.BetStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("bet-004");
    }

    @Test
    void givenNoMatchingBets_whenFindByEventIdAndStatus_thenReturnsEmptyList() {
        List<Bet> result = betRepository.findByEventIdAndStatus("event-999", Bet.BetStatus.PENDING);

        assertThat(result).isEmpty();
    }

    @Test
    void givenBet_whenSaved_thenCanBeRetrievedById() {
        Bet saved = betRepository.findById("bet-001").orElseThrow();

        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getEventId()).isEqualTo("event-1");
        assertThat(saved.getBetAmount()).isEqualByComparingTo("50.00");
        assertThat(saved.getStatus()).isEqualTo(Bet.BetStatus.PENDING);
    }

    @Test
    void givenPendingBet_whenStatusUpdatedToWon_thenNoLongerAppearsInPendingQuery() {
        Bet bet = betRepository.findById("bet-001").orElseThrow();
        bet.setStatus(Bet.BetStatus.WON);
        betRepository.save(bet);

        List<Bet> pending = betRepository.findByEventIdAndStatus("event-1", Bet.BetStatus.PENDING);

        assertThat(pending).extracting(Bet::getId).doesNotContain("bet-001");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Bet buildBet(String id, String userId, String eventId,
                          String winnerId, Bet.BetStatus status) {
        return Bet.builder()
                .id(id)
                .userId(userId)
                .eventId(eventId)
                .eventMarketId("market-1")
                .eventWinnerId(winnerId)
                .betAmount(new BigDecimal("50.00"))
                .status(status)
                .build();
    }
}
