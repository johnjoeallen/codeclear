// generated-from: review/python/raise-without-cause/rule.md
// rule-hash: 97a0891f020efd02b4502821e181f822a4857d73817420e5b4f9e74f81dd3c06
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'raise-without-cause'

def boundary = ['except_clause', 'function_definition', 'lambda', 'class_definition']

raise_statement.each { r ->
    skip file.isTest
    // Inside an except block, without leaving the function.
    def handler = r.ancestorOf(boundary)
    only handler != null && handler.type == 'except_clause'
    // "Do not flag": a bare raise, and a re-raise of a name (raise e): only a new exception is a problem.
    only r.children.any { x -> x.type == 'call' }

    def hidden = r.field('cause')
    // from None throws the cause away: fine when the message carries the original, or a comment says why.
    def carries = handler.descendants('as_pattern_target').any { t -> r.mentions(t.text) }

    report r, "A new exception is raised inside an except block without 'from': the original error is lost from the chain, so write 'from e'",
        hidden == null
    report r, "'from None' discards the original exception and the message does not use it: add a comment saying why, or chain it with 'from e'",
        hidden.type == 'none' && !carries && !r.hasNearbyComment
}
