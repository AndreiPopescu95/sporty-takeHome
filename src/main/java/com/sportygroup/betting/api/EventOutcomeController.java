package com.sportygroup.betting.api;

import com.sportygroup.betting.api.dto.EventOutcomeDto;
import com.sportygroup.betting.domain.model.Bet;
import com.sportygroup.betting.domain.repository.BetRepository;
import com.sportygroup.betting.kafka.EventOutcomeProducer;
import com.sportygroup.betting.service.UserLedger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the event outcome endpoint.
 *
 * Responsibility: Accept HTTP requests and delegate immediately to Kafka.
 * The controller does NOT contain business logic — that belongs in the
 * service/consumer layer. This keeps the controller thin and testable.
 *
 * WHY POST and not PUT/PATCH?
 *   Publishing an event outcome is a command ("record that this happened"),
 *   not an idempotent update — so POST is semantically correct.
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventOutcomeController {

    private final EventOutcomeProducer eventOutcomeProducer;
    private final BetRepository betRepository;
    private final UserLedger userLedger;

    /**
     * Publishes a sports event outcome to the Kafka "event-outcomes" topic.
     *
     * Example request body:
     * {
     *   "eventId": "event-1",
     *   "eventName": "Chelsea vs Arsenal",
     *   "eventWinnerId": "team-1"
     * }
     */
    @PostMapping("/outcome")
    public ResponseEntity<Map<String, String>> publishEventOutcome(
            @Valid @RequestBody EventOutcomeDto eventOutcome) {

        log.info("Received event outcome via API: eventId={}, winner={}",
                eventOutcome.getEventId(), eventOutcome.getEventWinnerId());

        eventOutcomeProducer.publish(eventOutcome);

        return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "message", "Event outcome published to Kafka",
                "eventId", eventOutcome.getEventId()
        ));
    }

    /**
     * Helper endpoint: view all bets in the DB (useful for testing settlement results).
     */
    @GetMapping("/bets")
    public ResponseEntity<List<Bet>> getAllBets() {
        return ResponseEntity.ok(betRepository.findAll());
    }

    /**
     * Helper endpoint: view current user balances in the in-memory ledger.
     * Shows credited winnings after settlement has run.
     */
    @GetMapping("/balances")
    public ResponseEntity<Map<String, BigDecimal>> getBalances() {
        return ResponseEntity.ok(userLedger.getAllBalances());
    }
}
