package com.sportygroup.betting.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a sports event outcome.
 *
 * Used:
 *   1. As the request body for POST /api/events/outcome
 *   2. As the Kafka message payload on the "event-outcomes" topic
 *
 * We use a single class for both to keep things simple.
 * In a production system you might separate the API contract from
 * the internal messaging contract to allow them to evolve independently.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOutcomeDto {

    @NotBlank(message = "eventId is required")
    @JsonProperty("eventId")
    private String eventId;

    @NotBlank(message = "eventName is required")
    @JsonProperty("eventName")
    private String eventName;

    @NotBlank(message = "eventWinnerId is required")
    @JsonProperty("eventWinnerId")
    private String eventWinnerId;
}
