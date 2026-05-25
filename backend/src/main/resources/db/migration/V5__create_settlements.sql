CREATE TABLE settlements (
    id                       BIGSERIAL PRIMARY KEY,
    room_id                  BIGINT  NOT NULL UNIQUE REFERENCES rooms(id),
    total_menu_amount        INTEGER NOT NULL,
    total_delivery_fee       INTEGER NOT NULL,
    participant_count        INTEGER NOT NULL,
    delivery_fee_per_person  INTEGER NOT NULL,
    host_surplus             INTEGER NOT NULL DEFAULT 0,
    created_at               TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE member_settlements (
    id                   BIGSERIAL PRIMARY KEY,
    settlement_id        BIGINT  NOT NULL REFERENCES settlements(id),
    member_id            BIGINT  NOT NULL REFERENCES members(id),
    menu_amount          INTEGER NOT NULL,
    delivery_fee_share   INTEGER NOT NULL,
    total_amount         INTEGER NOT NULL,
    is_host              BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_member_settlement_settlement ON member_settlements(settlement_id);
