class Inventory
{
    constructor(bot)
    {
        this.bot = bot
    }

    getItems()
    {
        return this.bot.inventory.items()
    }

    count(type)
    {
        const normalized = type.replace(/^minecraft:/, '')

        return this.getItems()
        .filter(item => item.name === normalized)
        .reduce((total, item) => total + item.count, 0)
    }

    has(type, amount = 1)
    {
        return this.count(type) >= amount
    }

    snapshot()
    {
        return this.getItems().map(item => ({
            type: item.name, amount: item.count
        }))
    }

    async clear()
    {
        for(const item of [...this.getItems()])
        {
            await this.bot.tossStack(item)
        }
    }

    async waitForItem(type, amount = 1, timeout = 5000)
    {
        const start = Date.now()

        while(Date.now() - start < timeout)
        {
            if(this.has(type, amount))
            {
                return true
            }

            await new Promise(resolve => setTimeout(resolve, 100))
        }

        return false
    }
}

module.exports = Inventory
