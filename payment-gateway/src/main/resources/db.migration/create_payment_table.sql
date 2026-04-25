
CREATE TYPE payment_status AS ENUM ('PENDING', 'PROCESSING', 'SETTLED', 'FAILED');
CREATE TYPE saga_step AS ENUM ('VALIDATION', 'DEBIT', 'CREDIT', 'LEDGER_ENTRY');

CREATE TABLE payments (
    id                      UUID            NOT NULL DEFAULT uuid_generate_v4(),
    idempotency_key         VARCHAR(64)     NOT NULL,
    source_account_id       UUID            NOT NULL,
    destination_account_id  UUID            NOT NULL,
    amount                  NUMERIC(19, 4)  NOT NULL,
    currency                CHAR(3)         NOT NULL,
    status_code             payment_status  NOT NULL DEFAULT 'PENDING',
    failure_reason          TEXT,
    failure_step            saga_step,
    reference               VARCHAR(256),
    metadata                JSONB,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    settled_at              TIMESTAMP,

    CONSTRAINT pk_payments              PRIMARY KEY (id),
    CONSTRAINT uq_payments_idempotency  UNIQUE (idempotency_key),
    CONSTRAINT chk_payment_amount       CHECK (amount > 0),
    CONSTRAINT chk_payment_currency     CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_different_accounts   CHECK (source_account_id != destination_account_id)
);

CREATE INDEX idx_payments_idempotency_key   ON payments (idempotency_key);
CREATE INDEX idx_payments_source_account    ON payments (source_account_id);
CREATE INDEX idx_payments_dest_account      ON payments (destination_account_id);
CREATE INDEX idx_payments_status            ON payments (status_code);
CREATE INDEX idx_payments_created_at        ON payments (created_at DESC);
CREATE INDEX idx_payments_settled_at        ON payments (settled_at DESC) WHERE settled_at IS NOT NULL;


-- Trigger: update updated_at
CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();