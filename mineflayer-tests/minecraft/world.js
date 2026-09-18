class World
{
    constructor(bot)
    {
        this.bot = bot
    }

    getBlock(position)
    {
        return this.bot.blockAt(position)
    }

    async teleportTo(position)
    {
        await this.bot.chat(`/tp ${this.bot.username} ${Math.floor(position.x)} ${Math.floor(position.y)} ${Math.floor(position.z)}`)
        await this.waitForPosition(position)
    }

    async waitForPosition(position, timeout = 5000)
    {
        const start = Date.now()

        const targetX = position.x + 0.5
        const targetY = position.y + 0.5
        const targetZ = position.z + 0.5

        while(Date.now() - start < timeout)
        {
            const current = this.bot.entity.position

            const distance = Math.sqrt(Math.pow(current.x - targetX, 2) + Math.pow(current.y - targetY, 2) + Math.pow(current.z - targetZ, 2))

            if(distance < 1)
            {
                return
            }

            await this.wait(50)
        }

        throw new Error(`Bot did not reach ` + `${position.x}, ${position.y}, ${position.z}`)
    }


    async waitForBlock(position, timeout = 5000)
    {
        await this.waitUntil(() =>
        {
            const block = this.getBlock(position)
            return block && block.name !== 'air'
        }, timeout, `Block not found at ${position.x}, ${position.y}, ${position.z}`)

        return this.getBlock(position)
    }

    async waitForAir(position, timeout = 5000)
    {
        await this.waitUntil(() =>
        {
            const block = this.getBlock(position)
            return !block || block.name === 'air'
        }, timeout, `Expected air at ${position.x}, ${position.y}, ${position.z}`)

        return true
    }

    async breakBlock(position, sneaking = false)
    {
        const block = this.getBlock(position)

        if(!block || block.name === 'air')
        {
            return
        }

        await this.teleportTo({
            x: position.x, y: position.y + 1, z: position.z
        })

        this.bot.setControlState('sneak', sneaking)

        try
        {
            await this.bot.dig(block)
        } finally
        {
            this.bot.setControlState('sneak', false)
        }

        await this.waitForAir(position)
    }

    async waitUntil(condition, timeout, message, interval = 100)
    {
        const start = Date.now()

        while(Date.now() - start < timeout)
        {
            if(await condition())
            {
                return
            }

            await this.wait(interval)
        }

        throw new Error(message)
    }

    wait(ms)
    {
        return new Promise(resolve => setTimeout(resolve, ms))
    }
}

module.exports = World
