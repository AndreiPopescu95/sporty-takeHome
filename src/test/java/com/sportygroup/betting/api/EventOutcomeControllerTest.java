package com.sportygroup.betting.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportygroup.betting.api.dto.EventOutcomeDto;
import com.sportygroup.betting.domain.repository.BetRepository;
import com.sportygroup.betting.kafka.EventOutcomeProducer;
import com.sportygroup.betting.service.UserLedger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller layer tests using @WebMvcTest.
 *
 * WHY @WebMvcTest and not @SpringBootTest?
 *   @WebMvcTest loads ONLY the web layer (controllers, filters, validation).
 *   It does not start Kafka, JPA, or any other infrastructure.
 *   This makes these tests fast (milliseconds) and focused purely on
 *   HTTP behaviour: routing, request validation, response codes and bodies.
 *   All dependencies of the controller are mocked with @MockBean.
 */
@WebMvcTest(EventOutcomeController.class)
class EventOutcomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventOutcomeProducer eventOutcomeProducer;

    @MockBean
    private BetRepository betRepository;

    @MockBean
    private UserLedger userLedger;

    // -------------------------------------------------------------------------
    // POST /api/events/outcome — happy path
    // -------------------------------------------------------------------------

    @Test
    void givenValidOutcome_whenPostOutcome_thenReturns202() throws Exception {
        EventOutcomeDto dto = EventOutcomeDto.builder()
                .eventId("event-1")
                .eventName("Chelsea vs Arsenal")
                .eventWinnerId("team-1")
                .build();

        mockMvc.perform(post("/api/events/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.eventId").value("event-1"));
    }

    @Test
    void givenValidOutcome_whenPostOutcome_thenProducerIsCalled() throws Exception {
        EventOutcomeDto dto = EventOutcomeDto.builder()
                .eventId("event-1")
                .eventName("Chelsea vs Arsenal")
                .eventWinnerId("team-1")
                .build();

        mockMvc.perform(post("/api/events/outcome")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));

        // Verify the producer was called with exactly the DTO we sent
        var captor = forClass(EventOutcomeDto.class);
        verify(eventOutcomeProducer, times(1)).publish(captor.capture());
        assert captor.getValue().getEventId().equals("event-1");
        assert captor.getValue().getEventWinnerId().equals("team-1");
    }

    // -------------------------------------------------------------------------
    // POST /api/events/outcome — validation failures (all should return 400)
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "invalid body: {0}")
    @ValueSource(strings = {
        // missing eventId
        "{\"eventName\":\"Chelsea vs Arsenal\",\"eventWinnerId\":\"team-1\"}",
        // missing eventName
        "{\"eventId\":\"event-1\",\"eventWinnerId\":\"team-1\"}",
        // missing eventWinnerId
        "{\"eventId\":\"event-1\",\"eventName\":\"Chelsea vs Arsenal\"}",
        // blank eventId
        "{\"eventId\":\"\",\"eventName\":\"Chelsea vs Arsenal\",\"eventWinnerId\":\"team-1\"}",
        // empty body
        "{}",
    })
    void givenInvalidBody_whenPostOutcome_thenReturns400AndProducerIsNeverCalled(String body) throws Exception {
        mockMvc.perform(post("/api/events/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventOutcomeProducer);
    }

    // -------------------------------------------------------------------------
    // GET /api/events/bets
    // -------------------------------------------------------------------------

    @Test
    void whenGetBets_thenReturns200() throws Exception {
        when(betRepository.findAll()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/events/bets"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // GET /api/events/balances
    // -------------------------------------------------------------------------

    @Test
    void whenGetBalances_thenReturns200WithLedgerData() throws Exception {
        when(userLedger.getAllBalances()).thenReturn(
                Map.of("user-1", new BigDecimal("95.00"),
                       "user-3", new BigDecimal("142.50"))
        );

        mockMvc.perform(get("/api/events/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['user-1']").value(95.00))
                .andExpect(jsonPath("$.['user-3']").value(142.50));
    }
}
