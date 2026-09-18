const ShopState = Object.freeze({
    OK: 'OK',
    EMPTY: 'EMPTY',
    OVERFILLED: 'OVERFILLED',
    ON_COOLDOWN: 'ON_COOLDOWN',
    LIMIT_REACHED: 'LIMIT_REACHED'
})

function createShopState({ inventory = [], state = ShopState.OK } = {}) {
    return { state, inventory }
}

module.exports = {
    ShopState,
    createShopState
}
