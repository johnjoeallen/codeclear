// generated-from: review/python/event-loop-ownership/rule.md
// rule-hash: 4a683b28d57d3cbed76fc8220eeecf302f390513bad3fe565fb274ddf11be1f1
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'event-loop-ownership'

call.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    def f = c.ancestorOf(['function_definition', 'lambda'])
    def module = c.ancestor('module')
    def name = f == null || f.type != 'function_definition' ? '' : f.field('name').text
    // A function that a thread or an executor runs: passed as target=, or to run_in_executor, to_thread or submit.
    def threaded = name != '' && module.descendants('call').any { k -> (k.keyword('target') != null && k.keyword('target').text == name)
        || (k.field('function').text ==~ /(\w+\.)*(run_in_executor|to_thread|submit|start_new_thread)/ && k.args.any { a -> a.text == name }) }

    candidate c, "${fn}() starts a new event loop inside a coroutine, which already runs in one: this raises RuntimeError or blocks the loop. Await the coroutine instead",
        fn in ['asyncio.run', 'loop.run_until_complete', 'asyncio.get_event_loop().run_until_complete'] && f != null && f.type == 'function_definition' && 'async' in f.tokens
    // "Do not flag": call_soon_threadsafe and run_coroutine_threadsafe, which are the safe ways in.
    candidate c, "${fn}() is called from a thread, but the event loop is not thread-safe: use loop.call_soon_threadsafe() or asyncio.run_coroutine_threadsafe()",
        fn ==~ /(\w+\.)*(call_soon|call_later|call_at|create_task|set_result|set_exception)/ && !(fn ==~ /.*threadsafe.*/) && threaded
}
