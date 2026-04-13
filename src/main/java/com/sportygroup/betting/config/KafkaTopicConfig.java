package com.sportygroup.betting.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration: declares topics so Spring auto-creates them on startup
 * if they don't already exist in the broker.
 *
 * WHY: Without this, Kafka would auto-create topics with default settings
 * (1 partition, replication factor 1). Declaring them explicitly lets us
 * control partitioning and replication, which matters for throughput and
 * fault-tolerance in production.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topic.event-outcomes}")
    private String eventOutcomesTopic;

    @Bean
    public NewTopic eventOutcomesTopic() {
        return TopicBuilder.name(eventOutcomesTopic)
                .partitions(3)   // 3 partitions allows up to 3 parallel consumers
                .replicas(1)     // 1 replica is fine for local dev; use 3 in production
                .build();
    }
}
