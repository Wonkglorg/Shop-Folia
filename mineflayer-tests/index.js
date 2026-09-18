const mineflayer = require('mineflayer')
const ShopBot = require('./ShopBot')
const tests = require('./test/shopTests')
const TestRunner = require('./test/runner')

async function main()
{
    const bot = mineflayer.createBot({
        host: '127.0.0.1', port: 25565, username: 'Bot', auth: 'offline', version: '26.2'
    })

    await new Promise((resolve, reject) =>
    {
        bot.once('spawn', resolve)
        bot.once('error', reject)
    })

    bot.on('message', message => {
        console.log(`[Server] ${message.toString()}`)
    })

    const shopBot = new ShopBot(bot)
    const runner = new TestRunner(shopBot)

    try
    {
        await runner.runAll(tests)
    } finally
    {
        if(typeof bot.quit === 'function')
        {
            bot.quit()
        }
    }
}

main().catch(error =>
{
    console.error(error)
    process.exitCode = 1
})