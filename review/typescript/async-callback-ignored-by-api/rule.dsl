// generated-from: review/typescript/async-callback-ignored-by-api/rule.md
// rule-hash: 8320f604f55fef53c50ff498f1efe661366923e5f41455d250b4c5b2a9d082bf
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'async-callback-ignored-by-api'

def asyncFn = { h -> h.type in ['arrow_function', 'function_expression'] && 'async' in h.tokens }
// "Do not flag": a body wrapped in one try/catch that reports the error.
def wrapped = { h -> !h.field('body').children.isEmpty() && h.field('body').children.every { s -> s.type == 'try_statement' } }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function')
    def name = fn.type == 'member_expression' ? fn.field('property').text : fn.text
    def full = fn.text

    candidate c, "${name}() does not wait for the promise that this async listener returns: a failure inside it is an unhandled rejection, and the emitter carries on as if the listener had finished. Catch errors inside the callback",
        name in ['on', 'once', 'addListener', 'prependListener', 'addEventListener'] && c.args.any { h -> asyncFn(h) && !h.descendants('await_expression').isEmpty() && !wrapped(h) }
    candidate c, "${name}() calls this callback and ignores the promise it returns, so a failure is an unhandled rejection and nothing waits for the work to finish",
        (full in ['setTimeout', 'setInterval', 'setImmediate', 'process.nextTick', 'queueMicrotask', 'window.setTimeout', 'window.setInterval'] && c.args.any { h -> asyncFn(h) && !wrapped(h) })
    // A promise is always truthy, so these array methods read every async result as true.
    candidate c, "${name}() uses the value the callback returns, but an async callback returns a promise, which is always truthy: the filter or the sort does not do what it says. Await the values first, with Promise.all",
        name in ['filter', 'some', 'every', 'find', 'findIndex', 'sort', 'reduce', 'findLast'] && fn.type == 'member_expression' && c.args.any { h -> asyncFn(h) }
}
