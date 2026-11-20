// generated-from: review/python/celery-dispatch-before-commit/rule.md
// rule-hash: d343c149095db956e80dbcb360f22f4d13e232d2738e65b53382dcccd1133214
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'celery-dispatch-before-commit'

// The worker can pick the task up before the transaction commits, and then look for a row that is not visible yet.
call.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    only fn ==~ /(\w+\.)*(delay|apply_async|send_task|s\.apply_async|si\.apply_async)/
    // "Do not flag": a dispatch wrapped in transaction.on_commit runs after the commit.
    skip c.ancestors('call').any { k -> k.field('function').text ==~ /(\w+\.)*on_commit/ }
    skip c.ancestors('lambda').any { l -> l.ancestors('call').any { k -> k.field('function').text ==~ /(\w+\.)*on_commit/ } }
    // Arguments that name something: a constant-only call has no row to wait for.
    only c.args.any { a -> !(a.type in ['integer', 'float', 'string', 'true', 'false', 'none']) }
    def inWith = c.ancestors('with_statement').any { w -> w.child('with_clause') != null && w.child('with_clause').text ==~ /(?s).*(atomic|\.begin\(|begin_nested).*/ }
    def decorated = c.ancestors('function_definition').any { f -> f.parent != null && f.parent.type == 'decorated_definition'
        && f.parent.children.any { d -> d.type == 'decorator' && d.text ==~ /(?s).*atomic.*/ } }

    candidate c, "${fn}() is called inside the transaction, so the worker can start before the commit and look for rows that are not visible yet (or that get rolled back). Use transaction.on_commit(lambda: ${fn}(...))",
        inWith || decorated
}
