package com.sportygroup.betting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory ledger tracking user account balances.
 *
 * In a production system this would be a call to a dedicated
 * Payment/Wallet service or a database table. Here we use a
 * ConcurrentHashMap so the state is visible across the application
 * (e.g. via the /api/events/balances endpoint) and thread-safe
 * under concurrent settlement messages.
 */
@Slf4j
@Component
public class UserLedger {

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    public void credit(String userId, BigDecimal amount) {
        balances.merge(userId, amount, BigDecimal::add);
        log.info("[Ledger] Credited {} to userId={} — new balance: {}",
                amount, userId, balances.get(userId));
    }

    public BigDecimal getBalance(String userId) {
        return balances.getOrDefault(userId, BigDecimal.ZERO);
    }

    public Map<String, BigDecimal> getAllBalances() {
        return Collections.unmodifiableMap(balances);
    }
}
