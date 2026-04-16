
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE account_status AS ENUM ('ACTIVE', 'SUSPENDED', 'CLOSED');

CREATE TABLE accounts (
    id                  UUID            NOT NULL DEFAULT uuid_generate_v4(),
    owner_id            UUID            NOT NULL,
    account_number      VARCHAR(34)     NOT NULL,
    currency            CHAR(3)         NOT NULL,
    available_balance   NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    ledger_balance      NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    held_amount         NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    status              account_status  NOT NULL DEFAULT 'ACTIVE',
    version             BIGINT          NOT NULL DEFAULT 0,    -- optimistic locking
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_accounts              PRIMARY KEY (id),
    CONSTRAINT uq_accounts_number       UNIQUE (account_number),
    CONSTRAINT chk_available_balance    CHECK (available_balance >= 0),
    CONSTRAINT chk_held_amount          CHECK (held_amount >= 0),
    CONSTRAINT chk_currency_format      CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_accounts_owner_id     ON accounts (owner_id);
CREATE INDEX idx_accounts_status       ON accounts (status);
CREATE INDEX idx_accounts_currency     ON accounts (currency);