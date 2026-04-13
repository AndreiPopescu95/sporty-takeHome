package com.sportygroup.betting.domain.repository;

import com.sportygroup.betting.domain.model.Bet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Bet entities.
 * Spring auto-generates all CRUD implementations at runtime.
 */
@Repository
public interface BetRepository extends JpaRepository<Bet, String> {

    /**
     * Find all bets for a given event that are still PENDING settlement.
     * Called by the Kafka consumer after receiving an event outcome.
     */
    List<Bet> findByEventIdAndStatus(String eventId, Bet.BetStatus status);
}
