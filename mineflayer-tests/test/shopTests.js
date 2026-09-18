const {
    item, buyShop
} = require('../shops/createShop')

const {ShopState} = require('../shops/shopState')

const SHOP_TESTS = [{
    name: 'Buy one item',

    setup: {
        shop: buyShop({
            position: {
                x: 0, y: -60, z: 0
            },

            item: item('minecraft:stone', 1),

            price: 10,

            inventory: [item('minecraft:stone', 32)]
        }),

        player: {
            inventory: [item('minecraft:diamond', 1)]
        }
    },

    action: {
        type: 'TRANSACT', amount: 1
    },

    expect: {
        result: 'SUCCESS',

        player: {
            inventory: [item('minecraft:stone', 1)]
        },

        shop: {
            state: ShopState.OK,

            inventory: [item('minecraft:stone', 31), item('minecraft:diamond', 1)]
        }
    }
}]

module.exports = SHOP_TESTS