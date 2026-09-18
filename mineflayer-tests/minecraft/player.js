class Player
{
    constructor(bot)
    {
        this.bot = bot
    }

    async clearInventory()
    {
        for(const item of [...this.bot.inventory.items()])
        {
            await this.bot.tossStack(item)
        }
    }

    async giveItem(type, amount = 1)
    {
        const item = type.replace(/^minecraft:/, '')
        const command = `/give ${this.bot.username} minecraft:${item} ${amount}`

        console.log(`[Command] ${command}`)

        await this.bot.chat(command)
    }

    async setExperience(level)
    {
        await this.bot.chat(`/experience set ${this.bot.username} ${level} levels`)
        await this.wait(250)
    }

    getExperience()
    {
        return {
            level: this.bot.experience.level, points: this.bot.experience.points, progress: this.bot.experience.progress
        }
    }

    wait(ms)
    {
        return new Promise(resolve => setTimeout(resolve, ms))
    }
}

module.exports = Player
