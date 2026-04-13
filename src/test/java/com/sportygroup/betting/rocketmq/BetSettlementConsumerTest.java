package com.sportygroup.betting.rocketmq;

import com.sportygroup.betting.api.dto.BetSettlementDto;
import com.sportygroup.betting.service.UserLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BetSettlementConsumer.
 *
 * WHY no mocks here at all?
 *   The consumer has one real dependency: UserLedger, which is a simple
 *   in-memory ConcurrentHashMap. There is no value in mocking it —
 *   we want to assert the actual balance values that result from
 *   consuming settlement messages. Using the real UserLedger makes
 *   these tests more meaningful and easier to read.
 */
class BetSettlementConsumerTest {

    private UserLedger userLedger;
    private BetSettlementConsumer consumer;

    @BeforeEach
    void setUp() {
        userLedger = new UserLedger();
        consumer = new BetSettlementConsumer(userLedger);
    }

    // -------------------------------------------------------------------------
    // Winning bets — payout calculation
    // -------------------------------------------------------------------------

    @Test
    void givenWonBet_whenConsume_thenUserBalanceIsCredited() {
        BetSettlementDto dto = buildSettlement("bet-001", "user-1", "WON", "50.00");

        consumer.consume(dto);

        // 50.00 * 1.90 = 95.00
        assertThat(userLedger.getBalance("user-1"))
                .isEqualByComparingTo("95.00");
    }

    @Test
    void givenWonBetWithLargeStake_whenConsume_thenPayoutIsCorrect() {
        BetSettlementDto dto = buildSettlement("bet-004", "user-1", "WON", "200.00");

        consumer.consume(dto);

        // 200.00 * 1.90 = 380.00
        assertThat(userLedger.getBalance("user-1"))
                .isEqualByComparingTo("380.00");
    }

    @Test
    void givenWonBetWithDecimalStake_whenConsume_thenPayoutIsRoundedCorrectly() {
        // 33.33 * 1.90 = 63.327 → should round to 63.33
        BetSettlementDto dto = buildSettlement("bet-005", "user-1", "WON", "33.33");

        consumer.consume(dto);

        assertThat(userLedger.getBalance("user-1"))
                .isEqualByComparingTo("63.33");
    }

    @Test
    void givenMultipleWonBets_whenConsumed_thenBalancesAccumulate() {
        consumer.consume(buildSettlement("bet-001", "user-1", "WON", "50.00"));  // +95.00
        consumer.consume(buildSettlement("bet-003", "user-1", "WON", "75.00"));  // +142.50

        // 95.00 + 142.50 = 237.50
        assertThat(userLedger.getBalance("user-1"))
                .isEqualByComparingTo("237.50");
    }

    @Test
    void givenWonBetsForDifferentUsers_whenConsumed_thenBalancesAreTrackedSeparately() {
        consumer.consume(buildSettlement("bet-001", "user-1", "WON", "50.00"));  // user-1: 95.00
        consumer.consume(buildSettlement("bet-003", "user-3", "WON", "75.00"));  // user-3: 142.50

        assertThat(userLedger.getBalance("user-1")).isEqualByComparingTo("95.00");
        assertThat(userLedger.getBalance("user-3")).isEqualByComparingTo("142.50");
    }

    // -------------------------------------------------------------------------
    // Losing bets — no payout
    // -------------------------------------------------------------------------

    @Test
    void givenLostBet_whenConsume_thenUserBalanceRemainsZero() {
        BetSettlementDto dto = buildSettlement("bet-002", "user-2", "LOST", "100.00");

        consumer.consume(dto);

        assertThat(userLedger.getBalance("user-2"))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void givenLostBet_whenConsume_thenLedgerHasNoEntryForUser() {
        BetSettlementDto dto = buildSettlement("bet-002", "user-2", "LOST", "100.00");

        consumer.consume(dto);

        // A user who only lost bets should have no entry in the ledger
        assertThat(userLedger.getAllBalances()).doesNotContainKey("user-2");
    }

    @Test
    void givenMixOfWonAndLostBets_whenConsumed_thenOnlyWinnersAreCredited() {
        consumer.consume(buildSettlement("bet-001", "user-1", "WON", "50.00"));
        consumer.consume(buildSettlement("bet-002", "user-2", "LOST", "100.00"));
        consumer.consume(buildSettlement("bet-003", "user-3", "WON", "75.00"));

        assertThat(userLedger.getBalance("user-1")).isEqualByComparingTo("95.00");
        assertThat(userLedger.getAllBalances()).doesNotContainKey("user-2");
        assertThat(userLedger.getBalance("user-3")).isEqualByComparingTo("142.50");
        assertThat(userLedger.getAllBalances()).hasSize(2);
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
