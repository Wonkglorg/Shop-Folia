const Vec3 = require('vec3')
const {ShopType} = require('./shopTypes')

const CREATION_LAYOUTS = {
    [ShopType.SELL]: {
        normal: ['shop', '%amount%', '%price%', 'sell'], admin: ['shop', '%amount%', '%price%', 'sell admin']
    }, [ShopType.BUY]: {
        normal: ['shop', '%amount%', '%price%', 'buy'], admin: ['shop', '%amount%', '%price%', 'buy admin']
    }, [ShopType.BARTER]: {
        normal: ['shop', '%amount%', '%price%', 'barter'], admin: ['shop', '%amount%', '%price%', 'barter admin']
    }, [ShopType.GAMBLE]: {
        admin: ['shop', '', '%price%', 'gamble']
    }
}

class ShopCreator
{
    constructor(bot, world, interaction, player)
    {
        this.bot = bot
        this.world = world
        this.interaction = interaction
        this.player = player
    }

    buildSignLines(shop)
    {
        const layout = CREATION_LAYOUTS[shop.type]?.[shop.admin ? 'admin' : 'normal']

        if(!layout)
        {
            throw new Error(`No creation layout for shop type: ${shop.type}`)
        }

        return layout.map(line => line.replace('%amount%', String(shop.amount)).replace('%price%', String(shop.price)))
    }

    async create(shop)
    {
        await this.player.clearInventory()
        const signPosition = new Vec3(shop.position.x, shop.position.y, shop.position.z)

        const containerPosition = this.getContainerPosition(signPosition, shop.facing)

        await this.ensureAir(signPosition)
        await this.world.waitForAir(containerPosition)

        await this.placeBlock('chest', containerPosition)

        const container = await this.world.waitForBlock(containerPosition)
        await this.populateContainer(containerPosition, shop.inventory)
        await this.placeSign(container, signPosition)

        const sign = await this.waitForSign(signPosition)

        await this.world.teleportTo(signPosition)
        await this.world.waitForPosition(signPosition)

        await this.writeSign(sign, this.buildSignLines(shop))
        await this.player.giveItem(shop.item.type, shop.item.amount)
        await this.initializeShop(signPosition, shop.item)
        await this.player.clearInventory()
        return {
            shop, signPosition, containerPosition
        }
    }


    async destroy(shop)
    {
        const signPosition = new Vec3(shop.position.x, shop.position.y, shop.position.z)
        const containerPosition = this.getContainerPosition(signPosition, shop.facing)

        await this.world.breakBlock(signPosition)
        await this.world.breakBlock(containerPosition)
    }

    getContainerPosition(signPosition, facing)
    {
        const direction = {
            NORTH: new Vec3(0, 0, 1), SOUTH: new Vec3(0, 0, -1), EAST: new Vec3(-1, 0, 0), WEST: new Vec3(1, 0, 0)
        }[String(facing).toUpperCase()]

        if(!direction)
        {
            throw new Error(`Unsupported shop facing: ${facing}`)
        }

        return signPosition.plus(direction)
    }

    async placeBlock(itemName, position)
    {
        const blockName = itemName.replace(/^minecraft:/, '')
        let block = this.bot.blockAt(position)
        if(block?.name === blockName)
        {
            return block
        }
        await this.bot.chat(`/setblock ${position.x} ${position.y} ${position.z} minecraft:${blockName}`)
        block = await this.world.waitForBlock(position)
        if(block.name !== blockName)
        {
            throw new Error(`Could not place ${blockName} at ` + `${position.x}, ${position.y}, ${position.z}; ` + `found ${block.name}`)
        }
        return block
    }

    async placeSign(container, signPosition)
    {
        const offset = signPosition.minus(container.position)

        const facing = {
            '0,-1,0': 'up', '0,1,0': 'down', '0,0,1': 'south', '0,0,-1': 'north', '1,0,0': 'east', '-1,0,0': 'west'
        }[`${offset.x},${offset.y},${offset.z}`]

        if(!facing || facing === 'up' || facing === 'down')
        {
            throw new Error(`Cannot place wall sign at ${signPosition.x}, ` + `${signPosition.y}, ${signPosition.z}: ` + `invalid attachment direction`)
        }

        await this.bot.chat(`/setblock ${signPosition.x} ${signPosition.y} ${signPosition.z} ` + `minecraft:oak_wall_sign[facing=${facing}]`)

        return await this.waitForSign(signPosition)
    }


    async writeSign(sign, lines)
    {
        const text = lines.map(line => String(line)).join('\n')
        console.log(`[ShopCreator] Writing sign at ` + `${sign.position.x}, ${sign.position.y}, ${sign.position.z}`)
        await this.bot.updateSign(sign, text)
        await this.wait(250)
    }

    async initializeShop(signPosition, shopItem)
    {
        const item = this.findInventoryItem(shopItem.type)

        if(!item)
        {
            throw new Error(`Bot does not have ${shopItem.type} to initialize the shop`)
        }

        await this.bot.equip(item, 'hand')

        const sign = await this.waitForSign(signPosition)

        await this.bot.lookAt(sign.position.offset(0.5, 0.5, 0.5), true)

        await this.interaction.leftClickBlock(sign)
    }


    async populateContainer(position, items)
    {
        if(!items?.length) return
        for(let slot = 0; slot < items.length; slot++)
        {
            const entry = items[slot]
            const itemName = entry.type.replace(/^minecraft:/, '')
            await this.bot.chat(`/item replace block ` + `${position.x} ${position.y} ${position.z} container.${slot} ` + `with minecraft:${itemName} ${entry.amount}`)
            await this.wait(100)
        }
    }

    async waitForSign(position, timeout = 5000)
    {
        await this.world.waitUntil(() =>
        {
            const block = this.world.getBlock(position)
            return block && this.isSign(block)
        }, timeout, `Sign not found at ${position.x}, ${position.y}, ${position.z}`)

        return this.world.getBlock(position)
    }

    isSign(block)
    {
        return block.name.endsWith('_sign') || block.name.endsWith('_wall_sign')
    }

    findInventoryItem(type)
    {
        const normalized = type.replace(/^minecraft:/, '')
        return this.bot.inventory.items().find(item => item.name === normalized)
    }

    async ensureAir(position)
    {
        const block = this.world.getBlock(position)

        if(block && block.name !== 'air')
        {
            throw new Error(`Expected air at ${position.x}, ${position.y}, ${position.z}, found ${block.name}`)
        }
    }

    wait(ms)
    {
        return new Promise(resolve => setTimeout(resolve, ms))
    }

    async prepareShop(shopConfig)
    {
        if(!shopConfig)
        {
            throw new Error('Shop configuration is required')
        }

        return await this.create(shopConfig)
    }
}

module.exports = ShopCreator
