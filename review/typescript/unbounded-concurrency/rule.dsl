// generated-from: review/typescript/unbounded-concurrency/rule.md
// rule-hash: 661133147ec2703ed04500c8e267930942662451d9ad27408581dcf256c853bc
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unbounded-concurrency'

def source = { t -> t ==~ /(?s).*(req\.|request\.|rows|results|records|files|readdir|\.find|query|items|ids|messages|users|list).*/ }

Node.each { n ->
    only n.type in ['call_expression', 'new_expression']
    skip file.isTest
    def module = n.ancestor('program')
    def f = n.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    def fn = n.type == 'call_expression' ? n.field('function').text : ''
    def handler = f != null && f.field('parameters') != null && f.field('parameters').text ==~ /(?s).*\b(req|request|res|ctx|message|msg|event|job)\b.*/
    // "Do not flag": a visible limiter or pool, and a small constant array.
    def limited = module.text ==~ /(?s).*(p-limit|pLimit|p-map|pMap|chunk|Semaphore|bottleneck|PQueue|p-queue|concurrency|batch|MAX_|\.length\s*>|\.slice\().*/
    def a0 = n.type == 'call_expression' ? n.arg(0) : null

    candidate n, "Promise.all over a list of unknown size starts every task at once: with a large list that opens thousands of connections or exhausts memory. Limit it with p-limit or process in chunks",
        fn == 'Promise.all' && a0 != null && a0.type == 'call_expression' && a0.field('function').type == 'member_expression' && a0.field('function').field('property').text == 'map'
            && a0.arg(0) != null && a0.arg(0).type in ['arrow_function', 'function_expression'] && 'async' in a0.arg(0).tokens && source(a0.field('function').field('object').text) && !limited
            && !(a0.field('function').field('object').type == 'array')
    candidate n, "A new worker or process is started for each request or message, so load decides how many run at once: use a pool with a fixed size",
        handler && !limited && ((n.type == 'new_expression' && n.field('constructor').text in ['Worker', 'worker_threads.Worker']) || fn in ['spawn', 'fork', 'child_process.spawn', 'child_process.fork', 'cp.fork', 'cp.spawn'])
    candidate n, "This data listener keeps every chunk with no size cap, so a large stream or subprocess output fills memory: count the bytes and stop past a limit",
        n.type == 'call_expression' && n.field('function').type == 'member_expression' && n.field('function').field('property').text == 'on' && n.arg(0) != null && n.arg(0).text ==~ /["']data["']/
            && n.arg(1) != null && n.arg(1).text ==~ /(?s).*(\.push\(|\+=|concat\().*/ && !(f.text ==~ /(?s).*(\.length|\.size|limit|MAX_|maxBuffer|destroy).*/)
    candidate n, "A listener is added inside a handler or a loop, so every call adds another one and none is removed: register it once, or remove it with off() when done",
        n.type == 'call_expression' && n.field('function').type == 'member_expression' && n.field('function').field('property').text in ['on', 'addListener'] && (handler || n.ancestorOf(['for_statement', 'for_of_statement', 'for_in_statement', 'while_statement', 'function_declaration', 'arrow_function', 'function_expression']) != null && n.ancestorOf(['for_statement', 'for_of_statement', 'for_in_statement', 'while_statement', 'function_declaration', 'arrow_function', 'function_expression']).type in ['for_statement', 'for_of_statement', 'for_in_statement', 'while_statement'])
            && !(f.text ==~ /(?s).*(\.off\(|removeListener|removeEventListener|\.once\().*/) && !limited
}
