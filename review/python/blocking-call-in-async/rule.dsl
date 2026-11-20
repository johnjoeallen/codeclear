// generated-from: review/python/blocking-call-in-async/rule.md
// rule-hash: 70a25a80facf624b3458a0440d31f0ad9fe174a51774931022877a9b98c0dc0e
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'blocking-call-in-async'

// Calls that block the event loop: the whole program waits while one of them runs.
def blocking = { t -> t == 'time.sleep' || t ==~ /requests\.\w+/ || t == 'urllib.request.urlopen' || t == 'os.system'
    || t ==~ /subprocess\.(run|call|check_call|check_output|Popen)/ || t ==~ /(psycopg2|pymysql|sqlite3|MySQLdb)\.connect/ }

call.each { c ->
    skip file.isTest
    // The nearest function must be an async def: "Do not flag" a nested plain def, a lambda (usually handed to an executor)
    // and a plain def endpoint, which the framework runs in a thread pool.
    def fn = c.ancestorOf(['function_definition', 'lambda'])
    only fn != null && fn.type == 'function_definition' && 'async' in fn.tokens
    // A function handed to asyncio.to_thread(...) or run_in_executor(...) is an argument, not a call, so it is never seen here.
    def callee = c.field('function').text
    def module = c.ancestor('module')
    def obj = c.field('function').field('object')
    // A call on a boto3 client or resource that the module creates is synchronous too.
    def boto = obj != null && module.descendants('assignment').any { a -> a.field('left').text == obj.text && a.field('right').type == 'call'
        && a.field('right').field('function').text in ['boto3.client', 'boto3.resource'] }

    report c, "${callee}() blocks the event loop inside an async function: use the async version, or run it with asyncio.to_thread()",
        blocking(callee)
    report c, "${callee}() is a boto3 call, which blocks the event loop inside an async function: use aioboto3, or asyncio.to_thread()",
        boto
}
