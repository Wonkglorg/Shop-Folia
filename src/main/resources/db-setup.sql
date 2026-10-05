CREATE TABLE IF NOT EXISTS players
(
    uuid        TEXT    NOT NULL PRIMARY KEY,
    name        TEXT,
    last_online INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shops
(
    shop_uuid                TEXT    NOT NULL PRIMARY KEY,
    owner_uuid               TEXT    NOT NULL,
    item                     TEXT    NOT NULL,
    price                    REAL    NOT NULL,
    amount                   INTEGER NOT NULL,
    last_known_stock_count   INTEGER NOT NULL,
    last_known_stock_status  TEXT    NOT NULL,
    shop_type                TEXT    NOT NULL,
    sign_facing              TEXT    NOT NULL,
    display_type             TEXT    NULL     DEFAULT NULL,
    fake_sign                INTEGER          DEFAULT 0,
    secondary_item           TEXT    NULL     DEFAULT NULL,
    creation_time            INTEGER NOT NULL,
    destroy_time             INTEGER NOT NULL DEFAULT 0,
    item_type                TEXT    NOT NULL,
    secondary_item_type      TEXT    NULL     DEFAULT NULL,
    custom_item_id           TEXT    NULL     DEFAULT NULL,
    custom_secondary_item_id TEXT    NULL     DEFAULT NULL,
    shop_world               TEXT    NOT NULL,
    shop_x                   INTEGER NOT NULL,
    shop_y                   INTEGER NOT NULL,
    shop_z                   INTEGER NOT NULL
);

--settings a shop can have, such as a limit to how many times it can be used per player or a cooldown
CREATE TABLE IF NOT EXISTS shop_settings
(
    -- the shop the setting applies to
    shop_uuid   TEXT NOT NULL,
    -- the unique key of the setting
    setting_key TEXT NOT NULL,
    -- the value of the setting
    value       TEXT,

    PRIMARY KEY (shop_uuid, setting_key),

    FOREIGN KEY (shop_uuid)
        REFERENCES shops (shop_uuid)
        ON DELETE CASCADE
);

--this table stores every transaction a player has done with a shop
CREATE TABLE IF NOT EXISTS transactions
(
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    -- the id of the shop
    shop_uuid         TEXT    NOT NULL,
    -- when the transaction happened
    timestamp         INTEGER NOT NULL,
    -- the user who did the transaction with the shop
    purchaser_uuid    TEXT    NOT NULL,
    -- if the transaction was gambling shows the reward the user got from gambling
    gamble_reward     TEXT    NULL,
    -- How many trades were done within this one transaction with the shop
    transaction_count INTEGER NOT NULL DEFAULT 1,

    FOREIGN KEY (shop_uuid)
        REFERENCES shops (shop_uuid)
        ON DELETE CASCADE
);

--lookups for the purchaser
CREATE INDEX IF NOT EXISTS idx_transactions_purchaser_shop
    ON transactions (purchaser_uuid, shop_uuid);

--lookups for latest time
CREATE INDEX IF NOT EXISTS idx_transactions_purchaser_shop_timestamp
    ON transactions (purchaser_uuid, shop_uuid, timestamp);

CREATE INDEX IF NOT EXISTS idx_transactions_purchaser_timestamp
    ON transactions (purchaser_uuid, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_timestamp
    ON transactions (timestamp DESC);

CREATE TABLE IF NOT EXISTS shop_actions
(
    timestamp     INTEGER NOT NULL,
    player_uuid   TEXT    NOT NULL,
    shop_uuid     TEXT    NOT NULL,
    player_action TEXT    NOT NULL,

    FOREIGN KEY (shop_uuid)
        REFERENCES shops (shop_uuid)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS currency_history
(
    timestamp     INTEGER NOT NULL,
    currency_type TEXT    NOT NULL,
    item          TEXT,

    PRIMARY KEY (timestamp)
);

CREATE VIEW IF NOT EXISTS shop_history AS

SELECT
    'TRANSACTION' AS source_type,

    t.timestamp,

    s.shop_type AS action,

    t.purchaser_uuid AS player_uuid,
    t.purchaser_uuid AS transactor_uuid,

    s.owner_uuid AS owner_uuid,

    p.name AS player_name,
    owner.name AS owner_name,

    s.shop_uuid,
    s.item,
    s.secondary_item,

    s.price,
    s.amount,
    t.transaction_count,
    t.gamble_reward,

    s.shop_world AS world_name,
    s.shop_x AS x,
    s.shop_y AS y,
    s.shop_z AS z

FROM transactions t
         JOIN shops s
              ON s.shop_uuid = t.shop_uuid
         LEFT JOIN players p
                   ON p.uuid = t.purchaser_uuid
         LEFT JOIN players owner
                   ON owner.uuid = s.owner_uuid

UNION ALL

SELECT
    'ACTION' AS source_type,

    sa.timestamp,

    sa.player_action AS action,

    sa.player_uuid AS player_uuid,
    NULL AS transactor_uuid,

    s.owner_uuid AS owner_uuid,

    p.name AS player_name,
    owner.name AS owner_name,

    s.shop_uuid,
    s.item,
    NULL AS secondary_item,

    s.price,
    s.amount,
    0 AS transaction_count,
    NULL AS gamble_reward,

    s.shop_world AS world_name,
    s.shop_x AS x,
    s.shop_y AS y,
    s.shop_z AS z

FROM shop_actions sa
         JOIN shops s
              ON s.shop_uuid = sa.shop_uuid
         LEFT JOIN players p
                   ON p.uuid = sa.player_uuid
         LEFT JOIN players owner
                   ON owner.uuid = s.owner_uuid;