CREATE TABLE IF NOT EXISTS players
(
    uuid TEXT NOT NULL PRIMARY KEY,
    name TEXT
);

CREATE TABLE IF NOT EXISTS shops
(
    shop_uuid               TEXT    NOT NULL,
    owner_uuid              TEXT    NOT NULL,
    item                    TEXT    NOT NULL,
    price                   REAL    NOT NULL,
    --if the shop is a combo shop this is the sell price part of the shop
    price_combo_sell        REAL    NULL     DEFAULT NULL,
    amount                  INTEGER NOT NULL,
    last_known_stock_count  INTEGER NOT NULL,
    last_known_stock_status TEXT    NOT NULL,
    destroyed_time          INTEGER NOT NULL DEFAULT 0,
    shop_type               TEXT    NOT NULL,
    sign_facing             TEXT    NOT NULL,
    display_type            TEXT    NULL     DEFAULT NULL,
    fake_sign               INTEGER          DEFAULT 0,
    barter_item             TEXT    NULL     DEFAULT NULL,
    creation_time           INTEGER NOT NULL,
    item_type               TEXT    NOT NULL,
    item_barter_type        TEXT    NULL     DEFAULT NULL,
    shop_world              TEXT    NOT NULL,
    shop_x                  INTEGER NOT NULL,
    shop_y                  INTEGER NOT NULL,
    shop_z                  INTEGER NOT NULL,
    PRIMARY KEY (shop_uuid, owner_uuid)
);

CREATE TABLE IF NOT EXISTS transactions
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    -- the id of the shop
    shop_uuid      TEXT    NOT NULL,
    -- when the transaction happened
    timestamp      INTEGER NOT NULL,
    -- the user who did the transaction with the shop
    purchaser_uuid TEXT    NOT NULL,
    -- if the value is set to one show it as an offline purchase the next time they log in
    cache_offline  INTEGER NOT NULL DEFAULT 0,
    -- if the transaction was gambling shows the reward the user got from gambling
    gamble_reward  TEXT,

    FOREIGN KEY (shop_uuid)
        REFERENCES shops (shop_uuid)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS shop_actions
(
    timestamp     TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    player_uuid   TEXT NOT NULL,
    owner_uuid    TEXT NOT NULL,
    shop_uuid     TEXT NOT NULL,
    player_action TEXT NOT NULL,

    FOREIGN KEY (shop_uuid)
        REFERENCES shops (shop_uuid)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS currency_history
(
    timestamp     TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    currency_type TEXT NOT NULL,
    item          TEXT,

    PRIMARY KEY (timestamp)
);