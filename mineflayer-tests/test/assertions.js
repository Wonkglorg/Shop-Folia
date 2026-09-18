function assertEqual(actual, expected, path = '')
{
    if(actual !== expected)
    {
        throw new Error(`${path || 'value'}: expected ${expected}, got ${actual}`)
    }
}

function assertInventory(actual, expected, path = 'inventory')
{
    const actualItems = normalizeInventory(actual)
    const expectedItems = normalizeInventory(expected)

    if(JSON.stringify(actualItems) !== JSON.stringify(expectedItems))
    {
        throw new Error(`${path}:\n` + `Expected: ${JSON.stringify(expectedItems, null, 2)}\n` + `Actual:   ${JSON.stringify(actualItems, null, 2)}`)
    }
}

function normalizeInventory(inventory = [])
{
    return [...inventory]
    .map(item => ({
        type: item.type, amount: item.amount
    }))
    .sort((a, b) =>
    {
        if(a.type < b.type) return -1
        if(a.type > b.type) return 1
        return a.amount - b.amount
    })
}

function assertPlayer(actual, expected)
{
    if(expected.balance !== undefined)
    {
        assertEqual(actual.balance, expected.balance, 'player.balance')
    }

    if(expected.experience !== undefined)
    {
        assertEqual(actual.experience, expected.experience, 'player.experience')
    }

    if(expected.inventory !== undefined)
    {
        assertInventory(actual.inventory, expected.inventory, 'player.inventory')
    }
}

function assertShop(actual, expected)
{
    if(expected.state !== undefined)
    {
        assertEqual(actual.state, expected.state, 'shop.state')
    }

    if(expected.inventory !== undefined)
    {
        assertInventory(actual.inventory, expected.inventory, 'shop.inventory')
    }
}

function assertResult(actual, expected)
{
    assertEqual(actual, expected, 'transaction.result')
}

function assertTestResult(actual, expected)
{
    if(expected.result !== undefined)
    {
        assertResult(actual.result, expected.result)
    }

    if(expected.player !== undefined)
    {
        assertPlayer(actual.player, expected.player)
    }

    if(expected.shop !== undefined)
    {
        assertShop(actual.shop, expected.shop)
    }
}

module.exports = {
    assertTestResult, assertPlayer, assertShop, assertInventory
}