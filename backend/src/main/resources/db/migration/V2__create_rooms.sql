CREATE TYPE room_status AS ENUM ('OPEN', 'CLOSED', 'CONFIRMED', 'COMPLETED', 'CANCELLED');
CREATE TYPE meeting_type AS ENUM ('DELIVERY', 'DELIVERY_TOGETHER', 'DINE_OUT');

CREATE TABLE rooms (
    id                    BIGSERIAL PRIMARY KEY,
    host_id               BIGINT         NOT NULL REFERENCES members(id),
    title                 VARCHAR(30)    NOT NULL,
    meeting_type          meeting_type   NOT NULL,
    restaurant_name       VARCHAR(100)   NOT NULL,
    restaurant_address    VARCHAR(255)   NOT NULL,
    restaurant_category   VARCHAR(50),
    latitude              DOUBLE PRECISION NOT NULL,
    longitude             DOUBLE PRECISION NOT NULL,
    delivery_fee          INTEGER        NOT NULL DEFAULT 0,
    max_participants      INTEGER        NOT NULL CHECK (max_participants >= 2),
    current_participant_count INTEGER    NOT NULL DEFAULT 1,
    status                room_status    NOT NULL DEFAULT 'OPEN',
    closed_at             TIMESTAMP,
    meeting_address       VARCHAR(255),
    account_number        VARCHAR(500),
    account_holder        VARCHAR(50),
    bank_name             VARCHAR(50),
    version               BIGINT         NOT NULL DEFAULT 0,
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rooms_status_created ON rooms(status, created_at DESC);
CREATE INDEX idx_rooms_filter ON rooms(status, restaurant_category, meeting_type);
CREATE INDEX idx_rooms_location ON rooms(latitude, longitude);
CREATE INDEX idx_rooms_auto_close ON rooms(status, closed_at) WHERE status = 'OPEN';
