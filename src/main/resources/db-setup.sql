CREATE TABLE IF NOT EXISTS players (
    uuid TEXT NOT NULL PRIMARY KEY,
    name TEXT
);

CREATE TABLE IF NOT EXISTS shops (
    shop_uuid TEXT NOT NULL PRIMARY KEY,
    owner_uuid TEXT NOT NULL,
    item TEXT NOT NULL,
    price REAL NOT NULL,
    amount INTEGER NOT NULL,
    active INTEGER NOT NULL,
    shop_type TEXT NOT NULL,
    barter_item TEXT,
    timestamp INTEGER NOT NULL,
    item_type TEXT,
    item_barter_type TEXT,
    shop_world TEXT NOT NULL,
    shop_x INTEGER NOT NULL,
    shop_y INTEGER NOT NULL,
    shop_z INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    -- the id of the shop
    shop_uuid TEXT NOT NULL,
    -- when the transaction happened
    timestamp INTEGER NOT NULL,
    -- the user who did the transaction with the shop
    purchaser_uuid TEXT NOT NULL,
    -- if the value is set to one show it as an offline purchase the next time they log in
    cache_offline INTEGER NOT NULL DEFAULT 0,
    -- if fractional sales are enabled how much fractions were bough 1 = full amount, 0 = nothing
    fraction REAL,
    -- if the transaction was gambling shows the reward the user got from gambling
    gamble_reward TEXT,

    FOREIGN KEY (shop_uuid)
        REFERENCES shops(shop_uuid)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS shop_actions (
    timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    player_uuid TEXT NOT NULL,
    owner_uuid TEXT NOT NULL,
    shop_uuid TEXT NOT NULL,
    player_action TEXT NOT NULL,

    FOREIGN KEY (shop_uuid)
        REFERENCES shops(shop_uuid)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS currency_history (
    timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    currency_type TEXT NOT NULL,
    item TEXT,

    PRIMARY KEY (timestamp)
);