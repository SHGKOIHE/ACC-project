CREATE TABLE members (
    id           BIGSERIAL PRIMARY KEY,
    nickname     VARCHAR(12)  NOT NULL UNIQUE,
    device_token VARCHAR(255) NOT NULL UNIQUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_member_device_token ON members(device_token);
CREATE INDEX idx_member_nickname ON members(nickname);
