CREATE TABLE IF NOT EXISTS shop_transaction
(
    id  INTEGER NOT NULL AUTO_INCREMENT,
    t_type CHAR(6) NOT NULL,
    price   DOUBLE NOT NULL,
    amount   SMALLINT NOT NULL,
    item   TEXT NOT NULL,
    barter_item   TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS shop_action
(
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    player_uuid     CHAR(36)  NOT NULL,
    owner_uuid     CHAR(36)  NOT NULL,
    shop_uuid     CHAR(36)  NOT NULL,
    shop_world   CHAR(128)  NOT NULL,
    shop_x   INTEGER  NOT NULL,
    shop_y   INTEGER  NOT NULL,
    shop_z   INTEGER  NOT NULL,
    player_action     CHAR(8)  NOT NULL,
    transaction_id INTEGER,
    FOREIGN KEY (transaction_id) REFERENCES shop_transaction(id)
);
