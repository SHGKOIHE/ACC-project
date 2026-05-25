ALTER TABLE members ADD COLUMN fcm_token VARCHAR(255);

CREATE TABLE chat_messages (
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT NOT NULL REFERENCES rooms(id),
    member_id  BIGINT REFERENCES members(id),
    type       VARCHAR(20) NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_room_created ON chat_messages(room_id, created_at DESC);
