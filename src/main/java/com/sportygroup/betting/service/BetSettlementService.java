package com.sportygroup.betting.service;

import com.sportygroup.betting.api.dto.BetSettlementDto;
import com.sportygroup.betting.api.dto.EventOutcomeDto;
import com.sportygroup.betting.domain.model.Bet;
import com.sportygroup.betting.domain.repository.BetRepository;
import com.sportygroup.betting.rocketmq.BetSettlementProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core business logic service.
 *
 * Responsibility:
 *   1. Look up all PENDING bets for the incoming event
 *   2. Determine WON/LOST for each bet
 *   3. Send a settlement message to RocketMQ for each bet
 *
 * WHY separate service from the Kafka consumer?
 *   The consumer is infrastructure (Kafka-specific). The service is domain logic.
 *   Keeping them separate means you can test the business rules without
 *   a running Kafka broker, and you could trigger settlement from other
 *   sources (e.g., a scheduled job, a different queue) without duplication.
 *
 * WHY @Transactional?
 *   The DB updates to individual bets should be atomic. If one update fails,
 *   we don't want a partial settlement. The transaction rolls back on any
 *   unchecked exception.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BetSettlementService {

    private final BetRepository betRepository;
    private final BetSettlementProducer betSettlementProducer;

    @Transactional
    public void processEventOutcome(EventOutcomeDto eventOutcome) {
        String eventId = eventOutcome.getEventId();
        String actualWinnerId = eventOutcome.getEventWinnerId();

        log.info("Processing outcome for eventId={}, winner={}", eventId, actualWinnerId);

        // Step 1: Find all pending bets for this event
        List<Bet> pendingBets = betRepository.findByEventIdAndStatus(eventId, Bet.BetStatus.PENDING);

        if (pendingBets.isEmpty()) {
            log.warn("No pending bets found for eventId={}", eventId);
            return;
        }

        log.info("Found {} pending bets for eventId={}", pendingBets.size(), eventId);

        // Step 2: For each bet, determine outcome and publish to RocketMQ
        for (Bet bet : pendingBets) {
            boolean isWinner = bet.getEventWinnerId().equals(actualWinnerId);
            Bet.BetStatus newStatus = isWinner ? Bet.BetStatus.WON : Bet.BetStatus.LOST;

            // Build the settlement message BEFORE updating DB
            // (so the message reflects the decision we just made)
            BetSettlementDto settlement = BetSettlementDto.builder()
                    .betId(bet.getId())
                    .userId(bet.getUserId())
                    .eventId(eventId)
                    .eventName(eventOutcome.getEventName())
                    .betWinnerId(bet.getEventWinnerId())
                    .actualWinnerId(actualWinnerId)
                    .betAmount(bet.getBetAmount())
                    .outcome(newStatus.name())
                    .build();

            // Step 3: Publish to RocketMQ
            betSettlementProducer.send(settlement);

            // Step 4: Update bet status in DB
            bet.setStatus(newStatus);
            betRepository.save(bet);

            log.info("Settled bet={} for user={}: outcome={}, amount={}",
                    bet.getId(), bet.getUserId(), newStatus, bet.getBetAmount());
        }
    }
}
