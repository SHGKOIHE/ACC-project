CREATE TABLE room_participants (
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT    NOT NULL REFERENCES rooms(id),
    member_id  BIGINT    NOT NULL REFERENCES members(id),
    joined_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (room_id, member_id)
);

CREATE INDEX idx_room_participant_room ON room_participants(room_id);
CREATE INDEX idx_room_participant_member ON room_participants(member_id);
