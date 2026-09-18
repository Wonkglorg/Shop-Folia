const {ShopType} = require('./shopTypes')

function item(type, amount = 1)
{
    if(typeof type === 'object' && type !== null)
    {
        return {
            type: type.type.replace(/^minecraft:/, ''), amount: type.amount ?? 1
        }
    }

    if(typeof type !== 'string' || !type)
    {
        throw new Error('Item type must be a non-empty string')
    }

    if(!Number.isInteger(amount) || amount < 1)
    {
        throw new Error('Item amount must be a positive integer')
    }

    return {
        type: type.replace(/^minecraft:/, ''), amount
    }
}

function createShop({
                        type,
                        position,
                        facing = 'NORTH',
                        admin = true,
                        item: shopItem,
                        secondaryItem = null,
                        amount = 1,
                        price,
                        inventory = [],
                        settings = {}
                    } = {})
{
    if(!Object.values(ShopType).includes(type))
    {
        throw new Error(`Invalid shop type: ${type}`)
    }

    if(!position || !['x', 'y', 'z'].every(key => Number.isFinite(position[key])))
    {
        throw new Error('Shop position must contain numeric x, y and z values')
    }

    if(!shopItem)
    {
        throw new Error('Shop item is required')
    }

    if(!Number.isInteger(amount) || amount < 1)
    {
        throw new Error('Shop amount must be a positive integer')
    }

    if(typeof price !== 'number' || !Number.isFinite(price) || price < 0)
    {
        throw new Error('Shop price must be a non-negative number')
    }

    return {
        type,
        position: {
            x: position.x, y: position.y, z: position.z
        },
        facing: String(facing).toUpperCase(),
        admin: Boolean(admin),
        item: item(shopItem),
        secondaryItem: secondaryItem ? item(secondaryItem) : null,
        amount,
        price,
        inventory: inventory.map(entry => item(entry.type, entry.amount)),
        settings
    }
}

function buyShop(options)
{
    return createShop({...options, type: ShopType.BUY})
}

function sellShop(options)
{
    return createShop({...options, type: ShopType.SELL})
}

function barterShop(options)
{
    return createShop({...options, type: ShopType.BARTER})
}

function gambleShop(options)
{
    return createShop({...options, type: ShopType.GAMBLE})
}

module.exports = {
    item, createShop, buyShop, sellShop, barterShop, gambleShop
}
