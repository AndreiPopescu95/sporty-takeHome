-- Seed some bets so we can immediately test settlement
-- Event 1: Chelsea vs Arsenal, winner candidates: team-1 , team-2
INSERT INTO bets (id, user_id, event_id, event_market_id, event_winner_id, bet_amount, status)
VALUES ('bet-001', 'user-1', 'event-1', 'market-1', 'team-1', 50.00, 'PENDING');

INSERT INTO bets (id, user_id, event_id, event_market_id, event_winner_id, bet_amount, status)
VALUES ('bet-002', 'user-2', 'event-1', 'market-1', 'team-2', 100.00, 'PENDING');

INSERT INTO bets (id, user_id, event_id, event_market_id, event_winner_id, bet_amount, status)
VALUES ('bet-003', 'user-3', 'event-1', 'market-2', 'team-1', 75.00, 'PENDING');

-- Event 2: Lakers vs Warriors
INSERT INTO bets (id, user_id, event_id, event_market_id, event_winner_id, bet_amount, status)
VALUES ('bet-004', 'user-1', 'event-2', 'market-3', 'team-lakers', 200.00, 'PENDING');

INSERT INTO bets (id, user_id, event_id, event_market_id, event_winner_id, bet_amount, status)
VALUES ('bet-005', 'user-4', 'event-2', 'market-3', 'team-warriors', 150.00, 'PENDING');
