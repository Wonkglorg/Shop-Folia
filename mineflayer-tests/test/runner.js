const {assertTestResult} = require('./assertions')

class TestRunner
{
    constructor(shopBot)
    {
        this.shopBot = shopBot
    }


    async runAll(tests)
    {
        let passed = 0
        let failed = 0

        console.log(`Running ${tests.length} test(s)...\n`)

        for(const test of tests)
        {
            try
            {
                await this.run(test)

                console.log(`PASS: ${test.name}\n`)
                passed++
            } catch(error)
            {
                console.error(`FAIL: ${test.name}`)
                console.error(error)
                console.log()

                failed++
            }
        }

        console.log('--------------------')
        console.log(`Passed: ${passed}`)
        console.log(`Failed: ${failed}`)
        console.log(`Total:  ${tests.length}`)

        if(failed > 0)
        {
            throw new Error(`${failed} test(s) failed`)
        }
    }


    async run(test)
    {
        console.log(`Running: ${test.name}`)
        let prepared = false
        try
        {
            await this.shopBot.prepare(test.setup)
            prepared = true
            const actual = await this.shopBot.execute(test.action)
            assertTestResult(actual, test.expect)
        } finally
        {
            if(prepared)
            {
                await this.shopBot.destroyShop()
            }
        }
    }
}


module.exports = TestRunner