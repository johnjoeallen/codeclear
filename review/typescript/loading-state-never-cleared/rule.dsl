// generated-from: review/typescript/loading-state-never-cleared/rule.md
// rule-hash: 46095a5111aa4f1876d584905a62c6b87f7a802885a22698f1ed2e3da49eb9d8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'loading-state-never-cleared'

def functionTypes = ['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program']

call_expression.each { c ->
    only c.field('function').type == 'identifier' && c.field('function').text ==~ /(?i)set\w*(loading|busy|pending|submitting|fetching)\w*/
    only c.arg(0) != null && c.arg(0).type == 'true'
    skip file.isTest
    def scope = c.ancestorOf(functionTypes)
    def name = c.field('function').text
    // Followed by something that waits: an await or a .then.
    only scope.descendants('await_expression').any { w -> w.line.isGreaterThanOrEqualTo(c.line) }
        || scope.descendants('call_expression').any { k -> k.field('function').type == 'member_expression' && k.field('function').field('property').text == 'then' }

    // "Do not flag": the flag is cleared in a finally (or .finally), or on both the success path and the failure path.
    def clears = scope.descendants('call_expression').any { k -> k.field('function').text == name && k.arg(0) != null && k.arg(0).type == 'false'
        && !k.ancestors('finally_clause').isEmpty() }
    skip clears
    skip scope.descendants('call_expression').any { k -> k.field('function').text == name && k.arg(0) != null && k.arg(0).type == 'false'
        && k.ancestors('call_expression').any { a -> a.field('function').type == 'member_expression' && a.field('function').field('property').text == 'finally' } }
    def onFailure = scope.descendants('call_expression').any { k -> k.field('function').text == name && k.arg(0) != null && k.arg(0).type == 'false'
        && (!k.ancestors('catch_clause').isEmpty() || k.ancestors('call_expression').any { a -> a.field('function').type == 'member_expression' && a.field('function').field('property').text == 'catch' }) }
    def onSuccess = scope.descendants('call_expression').any { k -> k.field('function').text == name && k.arg(0) != null && k.arg(0).type == 'false'
        && k.ancestors('catch_clause').isEmpty() && k.ancestors('finally_clause').isEmpty() }
    skip onFailure && onSuccess

    report c, "${name}(true) is never cleared when the awaited work fails, so the screen stays in its loading state forever: call ${name}(false) in a finally block"
}
