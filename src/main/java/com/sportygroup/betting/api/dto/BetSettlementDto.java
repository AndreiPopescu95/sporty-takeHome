package com.sportygroup.betting.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing a bet settlement message sent to RocketMQ topic "bet-settlements".
 *
 * Contains everything the settlement consumer needs to process the outcome
 * without making additional DB calls — a self-contained message design.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetSettlementDto {

    @JsonProperty("betId")
    private String betId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("eventName")
    private String eventName;

    @JsonProperty("betWinnerId")
    private String betWinnerId;

    @JsonProperty("actualWinnerId")
    private String actualWinnerId;

    @JsonProperty("betAmount")
    private BigDecimal betAmount;

    /** Determined before publishing: WON if betWinnerId == actualWinnerId, else LOST */
    @JsonProperty("outcome")
    private String outcome;
}
