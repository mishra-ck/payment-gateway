
CREATE TABLE idempotency_records (
    id                  UUID        NOT NULL DEFAULT uuid_generate_v4(),
    idempotency_key     VARCHAR(64) NOT NULL,
    payment_id          UUID,
    response_status     INTEGER     NOT NULL,
    response_body       TEXT        NOT NULL,
    request_hash        VARCHAR(64),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMP NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),

    CONSTRAINT pk_idempotency_records   PRIMARY KEY (id),
    CONSTRAINT uq_idempotency_key       UNIQUE (idempotency_key),
    CONSTRAINT chk_response_status      CHECK (response_status BETWEEN 100 AND 599)
);

CREATE INDEX idx_idem_key        ON idempotency_records (idempotency_key);
CREATE INDEX idx_idem_payment_id ON idempotency_records (payment_id) WHERE payment_id IS NOT NULL;
CREATE INDEX idx_idem_expires_at ON idempotency_records (expires_at);

