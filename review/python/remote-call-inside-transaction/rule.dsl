// generated-from: review/python/remote-call-inside-transaction/rule.md
// rule-hash: 5918419c45641c401f475da6b2758f6215757fefcbe9d80d6a05e215389a42f4
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'remote-call-inside-transaction'

def remote = { t -> t ==~ /(requests|httpx)\.\w+/ || t ==~ /\w*(session|client|http)\w*\.(get|post|put|patch|delete|head|request)/ || t ==~ /(\w+\.)*(publish|produce|apply_async|delay)/
    || t ==~ /\w*(producer)\w*\.send/ || t in ['time.sleep', 'asyncio.sleep'] || t ==~ /boto3\.client\(.*/ }

// A network call while a transaction is open holds its locks and connection for as long as the call takes.
call.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    only remote(fn)
    // "Do not flag": work handed to on_commit runs after the commit, so it is not inside the transaction.
    skip c.ancestors('call').any { k -> k.field('function').text ==~ /(\w+\.)*on_commit/ }
    // Inside "with transaction.atomic():", "with session.begin():", "with conn:" ...
    def inWith = c.ancestors('with_statement').any { w -> w.child('with_clause') != null && w.child('with_clause').text ==~ /(?s).*(atomic|\.begin\(|begin_nested|\bconn\b|connection|transaction).*/ }
    // ... or a function decorated with @transaction.atomic.
    def decorated = c.ancestors('function_definition').any { f -> f.parent != null && f.parent.type == 'decorated_definition'
        && f.parent.children.any { d -> d.type == 'decorator' && d.text ==~ /(?s).*atomic.*/ } }

    candidate c, "${fn}() is a network call or a wait inside an open transaction: it holds database locks and a connection for as long as it takes, and a failure after a write leaves side effects behind. Move it after the commit (transaction.on_commit)",
        inWith || decorated
}
