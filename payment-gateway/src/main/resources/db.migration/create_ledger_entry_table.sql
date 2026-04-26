

CREATE TABLE ledger_entries (
    id                  UUID                NOT NULL DEFAULT,
    journal_id          UUID                NOT NULL,
    payment_id          UUID                NOT NULL,
    account_id          UUID                NOT NULL,
    entry_type          ledger_entry_type   NOT NULL,
    amount              NUMERIC(19, 4)      NOT NULL,
    currency            CHAR(3)             NOT NULL,
    description         VARCHAR(512),
    is_reversal         BOOLEAN             NOT NULL DEFAULT FALSE,
    reversed_entry_id   UUID,
    entry_date          TIMESTAMP         NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_ledger_entries            PRIMARY KEY (id),
    CONSTRAINT chk_ledger_amount            CHECK (amount > 0),
    CONSTRAINT fk_reversed_entry            FOREIGN KEY (reversed_entry_id)
                                            REFERENCES ledger_entries(id)
);

CREATE INDEX idx_ledger_payment_id  ON ledger_entries (payment_id);
CREATE INDEX idx_ledger_account_id  ON ledger_entries (account_id);
CREATE INDEX idx_ledger_journal_id  ON ledger_entries (journal_id);
CREATE INDEX idx_ledger_entry_date  ON ledger_entries (entry_date DESC);
CREATE INDEX idx_ledger_type        ON ledger_entries (entry_type);
