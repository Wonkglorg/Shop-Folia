const ShopType = Object.freeze({
    BUY: 'BUY',
    SELL: 'SELL',
    BARTER: 'BARTER',
    GAMBLE: 'GAMBLE'
})

const CurrencyType = Object.freeze({
    BALANCE: 'BALANCE',
    EXPERIENCE: 'EXPERIENCE',
    ITEM: 'ITEM'
})

const ShopAction = Object.freeze({
    TRANSACT: 'TRANSACT',
    TRANSACT_FULL_STACK: 'TRANSACT_FULL_STACK'
})

module.exports = {
    ShopType,
    CurrencyType,
    ShopAction
}
