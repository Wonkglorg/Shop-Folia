const {
    item, buyShop
} = require('../shops/createShop')

const {ShopState} = require('../shops/shopState')

const SHOP_TESTS = [{
    name: 'Buy one item with item currency',

    setup: {
        shop: buyShop({
            position: {
                x: 0, y: -60, z: 0
            },

            item: item('minecraft:stone'), amount: 1,

            price: 10,

            inventory: [item('minecraft:stone', 32)]
        }),

        player: {
            inventory: [
            ]
        }
    },

    action: {
        type: 'TRANSACT', amount: 1
    },

    expect: {
        result: 'SUCCESS',

        player: {
            inventory: [
            ]
        },

        shop: {
            state: ShopState.OK,

            inventory: [item('minecraft:stone', 31)
            ]
        }
    }
}]

module.exports = SHOP_TESTS