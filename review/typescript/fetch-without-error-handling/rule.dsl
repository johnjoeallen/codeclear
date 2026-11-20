// generated-from: review/typescript/fetch-without-error-handling/rule.md
// rule-hash: fa4eb3ae75e01eeefdccfdab435cf408fa9d7289744d969b6e5a85d28e847085
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'fetch-without-error-handling'

def functionTypes = ['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program']

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    only fn in ['fetch', 'globalThis.fetch'] || fn ==~ /axios\.\w+/
    // "Do not flag": a call inside try, or with a .catch anywhere on its chain.
    def t = c.ancestors('try_statement').any { s -> s.field('body').descendants('call_expression').any { k -> k == c } }
    skip t
    skip c.ancestors('call_expression').any { a -> a.field('function').type == 'member_expression' && a.field('function').field('property').text == 'catch' }
    // "Do not flag": the function hands the promise back to its caller.
    def outer = c.parent != null && c.parent.type == 'await_expression' ? c.parent : c
    skip outer.parent != null && (outer.parent.type == 'return_statement' || (outer.parent.type == 'arrow_function' && outer.parent.field('body') == outer))
    def scope = c.ancestorOf(functionTypes)

    // fetch does not reject on an HTTP error, so the status has to be looked at too.
    report c, "fetch() is not in a try block or followed by .catch, and its status is never checked: a network error rejects, and an HTTP error such as 500 is silently treated as a success",
        fn in ['fetch', 'globalThis.fetch'] && !(scope.mentions('ok') || scope.mentions('status'))
    report c, "${fn}() can fail, and this function sets a loading flag but has no try/catch or .catch, so the flag is never cleared",
        fn ==~ /axios\.\w+/ && scope.descendants('call_expression').any { k -> k.field('function').text ==~ /set\w*(Loading|Busy|Pending|Submitting|Fetching)\w*/ }
}
