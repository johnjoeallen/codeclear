// generated-from: review/python/event-loop-ownership/rule.md
// rule-hash: 4a683b28d57d3cbed76fc8220eeecf302f390513bad3fe565fb274ddf11be1f1
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'event-loop-ownership'

// An asyncio primitive made at module level belongs to the first loop that uses it, and a second asyncio.run() starts another loop.
assignment.each { a ->
    only a.ancestorOf(['function_definition', 'class_definition', 'lambda']) == null
    skip file.isTest
    def right = a.field('right')
    only right.type == 'call' && right.field('function').text in ['asyncio.Lock', 'asyncio.Queue', 'asyncio.Event', 'asyncio.Semaphore', 'asyncio.Condition']
    // The module starts more than one loop.
    only a.ancestor('module').countOf('run').isGreaterThanOrEqualTo(2)

    candidate a, "This asyncio primitive is made at module level and the module calls asyncio.run() more than once: the object is tied to the first loop and fails with 'attached to a different loop' in the next. Create it inside the coroutine"
}
