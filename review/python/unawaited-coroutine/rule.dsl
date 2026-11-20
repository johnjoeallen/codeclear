// generated-from: review/python/unawaited-coroutine/rule.md
// rule-hash: 36d8e4b730c8645ec844fa6c868e8388c95e6666c5742d0a576c89802eff55e0
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unawaited-coroutine'

def asyncCall = { n -> n in ['asyncio.sleep', 'asyncio.gather', 'asyncio.wait_for', 'asyncio.wait', 'asyncio.shield'] }
def spawn = { n -> n in ['asyncio.create_task', 'asyncio.ensure_future', 'loop.create_task', 'asyncio.get_event_loop().create_task'] || n ==~ /(\w+\.)*(create_task|ensure_future)/ }

// An expression statement that is a call and nothing else: the coroutine is created and thrown away.
expression_statement.each { s ->
    skip file.isTest
    def c = s.child('call')
    only c != null
    def fn = c.field('function')
    def name = fn.type == 'attribute' ? fn.field('attribute').text : fn.text
    def module = s.ancestor('module')
    // A function defined async def in this module (a method called on self counts by its name).
    def local = module.descendants('function_definition').any { d -> 'async' in d.tokens && d.field('name').text == name }

    candidate s, "${fn.text}() is a coroutine that is never awaited: it does not run, and Python only warns when the object is destroyed. Write 'await ${fn.text}(...)'",
        asyncCall(fn.text) || (local && !spawn(fn.text))
    // "Do not flag": a task kept in a variable. A task made as a statement is a reference nobody holds.
    candidate s, "The task from ${fn.text}() is not kept: the event loop holds only a weak reference to it, so it can be garbage-collected before it finishes, and its exception is lost",
        spawn(fn.text)
}
