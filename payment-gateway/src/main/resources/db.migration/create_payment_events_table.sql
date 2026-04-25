
CREATE TABLE payment_events (
    id              UUID        NOT NULL DEFAULT uuid_generate_v4(),
    payment_id      UUID        NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    from_status     VARCHAR(20),
    to_status       VARCHAR(20) NOT NULL,
    actor           VARCHAR(128),
    detail          TEXT,
    correlation_id  VARCHAR(64),
    occurred_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_payment_events    PRIMARY KEY (id),
    CONSTRAINT fk_payment_events    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

CREATE INDEX idx_pevt_payment_id  ON payment_events (payment_id);
CREATE INDEX idx_pevt_occurred_at ON payment_events (occurred_at DESC);