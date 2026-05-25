CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT       NOT NULL REFERENCES rooms(id),
    member_id  BIGINT       NOT NULL REFERENCES members(id),
    menu_name  VARCHAR(50)  NOT NULL,
    quantity   INTEGER      NOT NULL CHECK (quantity >= 1),
    price      INTEGER      NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_room ON order_items(room_id);
CREATE INDEX idx_order_items_member ON order_items(room_id, member_id);
