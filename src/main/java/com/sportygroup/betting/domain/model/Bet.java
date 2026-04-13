package com.sportygroup.betting.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a bet placed by a user on a sports event.
 * Stored in the H2 in-memory database.
 */
@Entity
@Table(name = "bets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bet {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String eventMarketId;

    /**
     * The winner ID this bet was placed on.
     * Settlement logic compares this against the actual event winner.
     */
    @Column(nullable = false)
    private String eventWinnerId;

    @Column(nullable = false)
    private BigDecimal betAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BetStatus status;

    public enum BetStatus {
        PENDING,
        WON,
        LOST
    }
}
