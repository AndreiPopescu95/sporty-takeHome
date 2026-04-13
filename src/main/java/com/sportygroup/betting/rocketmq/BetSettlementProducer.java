package com.sportygroup.betting.rocketmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportygroup.betting.api.dto.BetSettlementDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simulated RocketMQ producer.
 *
 * WHY mock instead of real RocketMQ?
 *   The assignment explicitly permits mocking RocketMQ if setup is complex.
 *   Real RocketMQ requires a running NameServer and Broker — adding that to
 *   Docker Compose increases complexity and setup time considerably.
 *   The mock preserves the correct architectural boundary: the producer
 *   interface, message shape, and topic name are all real; only the transport
 *   is simulated. Swapping in the real SDK later is a one-file change.
 *
 * HOW to make this real (if asked):
 *   1. Add rocketmq-spring-boot-starter dependency
 *   2. Inject RocketMQTemplate
 *   3. Replace the log call with: rocketMQTemplate.convertAndSend(topic, settlement)
 *   4. Add rocketmq.name-server=localhost:9876 to application.properties
 *   5. Add RocketMQ NameServer + Broker to docker-compose.yml
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BetSettlementProducer {

    private final ObjectMapper objectMapper;
    private final BetSettlementConsumer betSettlementConsumer;

    @Value("${rocketmq.topic.bet-settlements}")
    private String topic;

    public void send(BetSettlementDto settlement) {
        try {
            String payload = objectMapper.writeValueAsString(settlement);
            log.info("[RocketMQ MOCK] Sending to topic='{}': {}", topic, payload);

            // Simulate the message being received by the consumer
            // In production this would be: rocketMQTemplate.convertAndSend(topic, settlement)
            simulateConsumer(settlement);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize BetSettlementDto: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send bet settlement message", e);
        }
    }

    /**
     * Simulates message delivery to the consumer in the same JVM.
     * In production, RocketMQ broker handles delivery asynchronously.
     */
    private void simulateConsumer(BetSettlementDto settlement) {
        log.info("[RocketMQ MOCK] Delivering message to BetSettlementConsumer (simulated async)");
        betSettlementConsumer.consume(settlement);
    }
}
