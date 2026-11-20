// generated-from: review/python/unbounded-concurrency/rule.md
// rule-hash: 68d9cedb48cdf5755aa7cb5511eb509dba0ae54cf94900337fbeef653dc75bc3
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unbounded-concurrency'

def small = { r -> r != null && (r.type in ['list', 'tuple', 'string'] || (r.type == 'call' && r.field('function').text == 'range' && r.arg(0) != null && r.arg(0).type == 'integer')) }

call.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    def name = c.field('function').type == 'attribute' ? c.field('function').field('attribute').text : fn
    // The loop or comprehension that makes the calls.
    def loop = c.ancestorOf(['for_statement', 'list_comprehension', 'generator_expression', 'set_comprehension', 'dictionary_comprehension', 'function_definition', 'lambda'])
    def source = loop == null ? null : (loop.type == 'for_statement' ? loop.field('right') : loop.child('for_in_clause') == null ? null : loop.child('for_in_clause').field('right'))

    // "Do not flag": a collection of literal or small constant size.
    candidate c, "gather(*[... for x in items]) starts one task for every item at once: with a large collection that opens thousands of connections or exhausts memory. Limit it with a Semaphore inside the task, or process in batches",
        fn == 'asyncio.gather' && c.args.any { a -> a.type == 'list_splat' && (!a.descendants('list_comprehension').isEmpty() || !a.descendants('generator_expression').isEmpty()) }
            && !c.args.any { a -> a.type == 'list_splat' && a.descendants('for_in_clause').any { f -> small(f.field('right')) } }

    candidate c, "${name}() is called for each element of a collection of unknown size, so all the tasks run at once: bound the concurrency with a Semaphore or a worker pool",
        name in ['create_task', 'submit', 'ensure_future', 'run_in_executor'] && loop != null && loop.type in ['for_statement', 'list_comprehension', 'generator_expression', 'set_comprehension']
            && !small(source)
    // "Do not flag": a queue with a size limit, which pushes back on the producer.
    candidate c, "This queue has no size limit, so a fast producer fills memory while the consumer is slow: pass maxsize",
        fn in ['asyncio.Queue', 'queue.Queue', 'Queue', 'queue.SimpleQueue'] && (c.args.isEmpty() || (c.keyword('maxsize') != null && c.keyword('maxsize').text == '0') || (c.arg(0) != null && c.arg(0).text == '0'))
}
