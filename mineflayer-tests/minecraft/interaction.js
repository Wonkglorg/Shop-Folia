class Interaction
{
    constructor(bot)
    {
        this.bot = bot
    }

    async clickBlock(position, type = 'right')
    {
        const block = this.bot.blockAt(position)

        if(!block)
        {
            throw new Error(`No block found at ${position.x}, ${position.y}, ${position.z}`)
        }

        if(type === 'right')
        {
            await this.bot.activateBlock(block)
            return
        }

        if(type === 'left')
        {
            await this.bot.attack(block)
            return
        }

        throw new Error(`Unknown interaction type: ${type}`)
    }

    rightClickBlock(position)
    {
        return this.clickBlock(position, 'right')
    }

    leftClickBlock(position)
    {
        return this.clickBlock(position, 'left')
    }

    clickShop(position, type = 'right')
    {
        return this.clickBlock(position, type)
    }
}

module.exports = Interaction
