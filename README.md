# Sports Betting Settlement Trigger Service

A Spring Boot application that simulates sports betting event outcome handling and bet settlement via **Kafka** and **RocketMQ**.

> **GitHub repository:** https://github.com/AndreiPopescu95/sporty-takeHome

---

## Architecture Overview

```
HTTP POST /api/events/outcome
        │
        ▼
EventOutcomeController
        │  publishes EventOutcomeDto
        ▼
[Kafka Topic: event-outcomes]
        │
        ▼
EventOutcomeConsumer
        │  delegates to
        ▼
BetSettlementService
        │  looks up PENDING bets by eventId
        │  determines WON / LOST per bet
        │  sends BetSettlementDto
        ▼
[RocketMQ Topic: bet-settlements]  ← mocked (logs payload)
        │
        ▼
BetSettlementConsumer
        │  calculates payout, credits user ledger
        ▼
  [H2 in-memory DB — bet status updated to WON/LOST]
```

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Language |
| Spring Boot 3.3 | Application framework |
| Spring Kafka | Kafka producer/consumer abstraction |
| H2 (in-memory) | Database for bets — no setup needed |
| Spring Data JPA | ORM / repository layer |
| RocketMQ | Mocked — broker delivery simulated in-process |
| Docker Compose | Runs Kafka + Zookeeper locally |
| Lombok | Reduces boilerplate |

---

## Prerequisites

- **Java 21**
- **Maven 3.8+**
- **Docker + Docker Compose**

---

## How to Run

**Step 1 — Start Kafka:**
```bash
docker-compose up -d
```

**Step 2 — Start the application:**
```bash
# If you have Maven installed globally:
mvn spring-boot:run

# Or using the Maven wrapper (if generated via Spring Initializr):
./mvnw spring-boot:run
```

The app starts on **http://localhost:8081**. On startup, 5 bets are automatically seeded into the database — no manual setup needed.

---

## How to Use

Use either the **Postman collection** (`SportsBetting.postman_collection.json`) or the curl commands below.

### Publish an event outcome

**Postman:** `1. Publish Event Outcome → Settle event-1 — team-1 wins (Chelsea)`

```bash
curl -X POST http://localhost:8081/api/events/outcome \
  -H "Content-Type: application/json" \
  -d '{"eventId":"event-1","eventName":"Chelsea vs Arsenal","eventWinnerId":"team-1"}'
```

This triggers the full flow: Kafka → settlement service → RocketMQ (mocked) → payout. Watch the logs to see each step fire in sequence.

### View bet statuses

**Postman:** `2. View State → Get all bets`

```bash
curl http://localhost:8081/api/events/bets
```

Bets for `event-1` will now show `WON` or `LOST`. Bets for `event-2` remain `PENDING` — they are unaffected until their own outcome is published.

### View user balances

**Postman:** `2. View State → Get user balances`

```bash
curl http://localhost:8081/api/events/balances
```

After settling `event-1` with `team-1` as winner, users who bet on `team-1` will have a credited payout (stake × 1.90). Users who bet on the losing side will have no entry.

### H2 database console

Visit **http://localhost:8081/h2-console**
- JDBC URL: `jdbc:h2:mem:betsdb`
- Username: `sa`
- Password: *(leave empty)*

### Kafka UI

Visit **http://localhost:8080** to browse topics and inspect messages on `event-outcomes` visually.

---

## Running Tests

```bash
# Global Maven:
mvn test

# Or with the wrapper:
./mvnw test
```

No Docker needed — tests mock all infrastructure and use an embedded H2 database.

---

## Project Structure

```
src/main/java/com/sportygroup/betting/
├── SportsBettingApplication.java      # Entry point
├── api/
│   ├── EventOutcomeController.java    # REST endpoints
│   └── dto/
│       ├── EventOutcomeDto.java       # API + Kafka message shape
│       └── BetSettlementDto.java      # RocketMQ message shape
├── config/
│   └── KafkaTopicConfig.java          # Topic declarations
├── domain/
│   ├── model/Bet.java                 # JPA entity
│   └── repository/BetRepository.java  # Spring Data repository
├── kafka/
│   ├── EventOutcomeProducer.java      # Publishes to Kafka
│   └── EventOutcomeConsumer.java      # Listens from Kafka
├── rocketmq/
│   ├── BetSettlementProducer.java     # Sends to RocketMQ (mocked)
│   └── BetSettlementConsumer.java     # Consumes from RocketMQ (mocked)
└── service/
    ├── BetSettlementService.java      # Core business logic
    └── UserLedger.java                # In-memory user balance tracker
```
