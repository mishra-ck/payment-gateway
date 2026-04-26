
-- Parent table — no data stored here directly
CREATE TABLE transactions (
    id              UUID                NOT NULL DEFAULT uuid_generate_v4(),
    payment_id      UUID                NOT NULL,
    account_id      UUID                NOT NULL,
    type            transaction_type    NOT NULL,
    amount          NUMERIC(19, 4)      NOT NULL,
    currency        CHAR(3)             NOT NULL,
    balance_after   NUMERIC(19, 4)      NOT NULL,
    description     VARCHAR(512),
    reference       VARCHAR(256),
    correlation_id  VARCHAR(64),
    occurred_at     TIMESTAMP         NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_txn_amount   CHECK (amount > 0)
) PARTITION BY RANGE (occurred_at);

-- Indexes on parent propagate to all partitions automatically
CREATE INDEX idx_txn_payment_id    ON transactions (payment_id);
CREATE INDEX idx_txn_account_id    ON transactions (account_id);
CREATE INDEX idx_txn_occurred_at   ON transactions (occurred_at DESC);
CREATE INDEX idx_txn_type          ON transactions (type);
CREATE INDEX idx_txn_account_date  ON transactions (account_id, occurred_at DESC);
