ALTER TABLE carts
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE cart_items
    RENAME COLUMN menu_id TO menu_item_id;
