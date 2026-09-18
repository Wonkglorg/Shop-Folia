const Player = require('./minecraft/player')
const Inventory = require('./minecraft/inventory')
const World = require('./minecraft/world')
const Interaction = require('./minecraft/interaction')
const ShopCreator = require("./shops/shopCreator");

class ShopBot
{

    constructor(bot)
    {
        this.bot = bot

        this.player = new Player(bot)
        this.inventory = new Inventory(bot)
        this.world = new World(bot)
        this.interaction = new Interaction(bot)
        this.shopCreator = new ShopCreator(bot, this.world, this.interaction, this.player)
    }

    /**
     * Prepare Minecraft for a test.
     *
     * @param {Object} setup
     * @param {Object} setup.shop
     * @param {Object} setup.player
     */
    async prepare(setup)
    {
        if(!setup)
        {
            throw new Error('Test setup is required')
        }

        await this.shopCreator.prepareShop(setup.shop)
        console.log(`[Inventory] ${JSON.stringify(this.bot.inventory.items().map(stack => ({
            name: stack.name, count: stack.count
        })))}`)
        await this.preparePlayer(setup.player)
        console.log(`[Inventory] ${JSON.stringify(this.bot.inventory.items().map(stack => ({
            name: stack.name, count: stack.count
        })))}`)
    }

    /**
     * Prepare the player state.
     */
    async preparePlayer(playerConfig = {})
    {
        await this.player.clearInventory()

        if(playerConfig.inventory)
        {
            for(const item of playerConfig.inventory)
            {
                await this.player.giveItem(item.type, item.amount)
            }
        }

        if(playerConfig.experience !== undefined)
        {
            await this.player.setExperience(playerConfig.experience)
        }

        if(playerConfig.balance !== undefined)
        {
            await this.setBalance(playerConfig.balance)
        }
    }

    /**
     * Execute the requested test action.
     */
    async execute(action)
    {
        if(!action)
        {
            throw new Error('Test action is required')
        }

        switch(action.type)
        {
            case 'TRANSACT':
                return this.transact()

            case 'TRANSACT_FULL_STACK':
                return this.transactFullStack()

            default:
                throw new Error(`Unknown action type: ${action.type}`)
        }
    }

    /**
     * Perform a normal shop transaction.
     */
    async transact()
    {
        const shop = await this.findPreparedShop()

        await this.interaction.clickShop(shop.position, 'right')

        await this.waitForTransaction()

        return this.captureResult()
    }

    /**
     * Perform a full-stack transaction.
     */
    async transactFullStack()
    {
        const shop = await this.findPreparedShop()

        await this.interaction.clickShop(shop.position, 'right')

        await this.waitForTransaction()

        return this.captureResult()
    }

    /**
     * Capture the state needed by the test assertions.
     */
    async captureResult()
    {
        return {
            result: await this.getTransactionResult(),

            player: await this.capturePlayerState(),

            shop: await this.captureShopState()
        }
    }

    async capturePlayerState()
    {
        return {
            balance: await this.getBalance(),

            experience: await this.player.getExperience(),

            inventory: await this.inventory.snapshot()
        }
    }

    async captureShopState()
    {
        const shop = await this.findPreparedShop()

        return {
            state: await this.getShopState(shop),

            inventory: await this.getShopInventory(shop)
        }
    }

    /**
     * Locate the shop prepared for the current test.
     */
    async findPreparedShop()
    {
        if(!this.currentShop)
        {
            throw new Error('No shop has been prepared')
        }

        return this.currentShop
    }

    async waitForTransaction(timeout = 2000)
    {
        await this.wait(timeout)
    }

    async getTransactionResult()
    {
        if(!this.lastTransactionResult)
        {
            throw new Error('No transaction result available')
        }

        return this.lastTransactionResult
    }

    async getShopState(shop)
    {
        const inventory = await this.getShopInventory(shop)

        if(inventory.length === 0)
        {
            return ShopState.EMPTY
        }

        // The actual shop state is plugin-owned, so this should eventually
        // be determined from the shop interaction/result rather than inferred
        // purely from the inventory.
        return ShopState.OK
    }

    async getShopInventory(shop)
    {
        const containerPosition = this.shopCreator.getContainerPosition(new Vec3(shop.position.x, shop.position.y, shop.position.z), shop.facing)

        await this.world.teleportTo({
            x: containerPosition.x, y: containerPosition.y + 1, z: containerPosition.z
        })

        const block = await this.world.getBlock(containerPosition)

        if(!block)
        {
            throw new Error(`Shop container not found at ` + `${containerPosition.x}, ` + `${containerPosition.y}, ` + `${containerPosition.z}`)
        }

        const container = await this.bot.openContainer(block)

        try
        {
            return container.containerItems().map(stack => ({
                type:

                    `minecraft:${stack.name}`

                , amount: stack.count
            }))
        } finally
        {
            container.close()
        }
    }

    async destroyShop()
    {
        await this.shopCreator.destroy(await this.findPreparedShop())
    }

    async getBalance()
    {
        // Balance is server/plugin state and is not exposed by Mineflayer.
        // This should eventually use whatever economy integration the test
        // server provides.
        throw new Error('Balance inspection is not implemented yet')
    }

    async setBalance(amount)
    {
        if(!Number.isFinite(amount))
        {
            throw new Error('Balance must be a finite number')
        }

        this.bot.chat(`/balance set ${this.bot.username} ${amount}`)

        await this.wait(250)
    }
}

module.exports = ShopBot