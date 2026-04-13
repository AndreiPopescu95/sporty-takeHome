package com.sportygroup.betting.rocketmq;

import com.sportygroup.betting.api.dto.BetSettlementDto;
import com.sportygroup.betting.service.UserLedger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Simulated RocketMQ consumer for the "bet-settlements" topic.
 *
 * In a real system this would be annotated with @RocketMQMessageListener
 * and implement RocketMQListener<BetSettlementDto>.
 *
 * Responsibility:
 *   - Calculate the payout for winning bets
 *   - Credit winnings to the user's account via UserLedger
 *   - Log the settlement result for both winners and losers
 *
 * WHY is the payout logic here and not in BetSettlementService?
 *   BetSettlementService decides WHO won/lost (domain logic).
 *   This consumer handles WHAT HAPPENS as a result (side effects: crediting accounts).
 *   Separating them means this consumer can retry independently if the ledger
 *   is temporarily unavailable, without re-running the bet matching logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BetSettlementConsumer {

    /**
     * Fixed odds multiplier applied to winning bets.
     * In production this would be stored per-bet at placement time
     * (odds can change between placement and settlement).
     */
    private static final BigDecimal WIN_MULTIPLIER = new BigDecimal("1.90");

    private final UserLedger userLedger;

    /**
     * Process a single bet settlement message.
     * Called directly by BetSettlementProducer in the mock setup;
     * would be called by the RocketMQ broker in production.
     */
    public void consume(BetSettlementDto settlement) {
        log.info("[RocketMQ Consumer] Processing settlement: betId={}, userId={}, outcome={}",
                settlement.getBetId(), settlement.getUserId(), settlement.getOutcome());

        if ("WON".equals(settlement.getOutcome())) {
            processWin(settlement);
        } else {
            processLoss(settlement);
        }

        log.info("[RocketMQ Consumer] Settlement complete for betId={}", settlement.getBetId());
    }

    private void processWin(BetSettlementDto settlement) {
        BigDecimal payout = settlement.getBetAmount()
                .multiply(WIN_MULTIPLIER)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("[RocketMQ Consumer] BET WON — betId={}, userId={}, event='{}', " +
                        "betOn={}, actualWinner={}, stake={}, payout={}",
                settlement.getBetId(),
                settlement.getUserId(),
                settlement.getEventName(),
                settlement.getBetWinnerId(),
                settlement.getActualWinnerId(),
                settlement.getBetAmount(),
                payout);

        userLedger.credit(settlement.getUserId(), payout);

        log.info("[RocketMQ Consumer] Credited {} to userId={} — running balance: {}",
                payout, settlement.getUserId(), userLedger.getBalance(settlement.getUserId()));
    }

    private void processLoss(BetSettlementDto settlement) {
        log.info("[RocketMQ Consumer] BET LOST — betId={}, userId={}, event='{}', " +
                        "betOn={}, actualWinner={}, stake={} — no payout",
                settlement.getBetId(),
                settlement.getUserId(),
                settlement.getEventName(),
                settlement.getBetWinnerId(),
                settlement.getActualWinnerId(),
                settlement.getBetAmount());
    }
}
