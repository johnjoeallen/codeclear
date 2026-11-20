// generated-from: review/typescript/swallowed-error-or-false-success/rule.md
// rule-hash: c7bd6191188d7a13f5e38cdef9c71429cf528842a23d49716d6b7403077fd665
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'swallowed-error-or-false-success'

def logOnly = { s -> s.type == 'comment' || (s.type == 'expression_statement' && s.text ==~ /(?s)\s*(console|logger|log|this\.logger)\.\w+\(.*/)
    || (s.type == 'return_statement' && s.text ==~ /(?s)\s*return\s*(true|\[\]|\{\}|null|undefined|false|0|"")?\s*;?\s*/) }
def literalValue = { v -> v == null || v.type in ['null', 'undefined', 'false', 'true', 'array', 'object', 'number'] || (v.type == 'string') }

Node.each { n ->
    only n.type in ['catch_clause', 'call_expression']
    skip file.isTest
    def body = n.type == 'catch_clause' ? n.field('body') : n
    def fn = n.type == 'call_expression' ? n.field('function') : n

    // "Do not flag": a catch that rethrows or wraps, one in a finally block (best-effort cleanup), and a boundary that answers the caller.
    candidate n, "This catch block is empty, so the failure disappears: the code carries on as if the call had worked. Handle it, log it with the error, or rethrow",
        n.type == 'catch_clause' && body.children.every { s -> s.type == 'comment' } && n.ancestors('finally_clause').isEmpty()
    candidate n, "This catch block only logs and then carries on or returns a success-shaped value (true, [], {}): the caller cannot tell that the call failed. Rethrow, or return an error the caller can see",
        n.type == 'catch_clause' && !body.children.isEmpty() && body.children.every(logOnly) && body.children.any { s -> s.type == 'return_statement' || s.type == 'expression_statement' }
            && body.descendants('throw_statement').isEmpty() && n.ancestors('finally_clause').isEmpty() && !(body.text ==~ /(?s).*\b(res|reply|next|ctx|callback|cb)\b.*/)
    // A .catch that turns the failure into a value.
    candidate n, ".catch() turns the failure into an ordinary value, so the caller cannot tell it failed: let it reject, or return an error the caller checks",
        n.type == 'call_expression' && fn.type == 'member_expression' && fn.field('property').text == 'catch' && n.arg(0) != null && n.arg(0).type == 'arrow_function'
            && literalValue(n.arg(0).field('body')) && !(n.arg(0).field('body').type == 'string' && n.arg(0).field('body').text ==~ /(?s)["'`]\w+ .*/)
    candidate n, ".then(ok, () => value) replaces the failure with an ordinary value, so the caller cannot tell it failed",
        n.type == 'call_expression' && fn.type == 'member_expression' && fn.field('property').text == 'then' && n.arg(1) != null && n.arg(1).type == 'arrow_function'
            && n.arg(1).field('body').type in ['null', 'undefined', 'false', 'true', 'array', 'object', 'number']
}
