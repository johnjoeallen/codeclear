// generated-from: review/typescript/error-cause-lost/rule.md
// rule-hash: b1e8000408751b1fbb2e167ab93f821f1960a92aac1fa608198b957f8d90e70b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'error-cause-lost'

def logCall = { k -> k.field('function').text ==~ /(console|logger|log|this\.logger|this\.log)\.(error|warn|info|log|debug)/ }

new_expression.each { n ->
    skip file.isTest
    // A new error that is thrown, or handed to reject(...), inside a catch block (not inside a function declared in it).
    def thrown = n.parent != null && n.parent.type == 'throw_statement'
    def rejected = n.parent != null && n.parent.type == 'arguments' && n.parent.parent.field('function').text == 'reject'
    only thrown || rejected
    def handler = n.ancestorOf(['catch_clause', 'function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'class_declaration'])
    only handler != null && handler.type == 'catch_clause'

    // "Do not flag": the error uses the caught one (cause: e, e.message, e passed on) ...
    def caught = handler.field('parameter')
    skip caught != null && n.mentions(caught.text)
    // ... and a security boundary that hides the details on purpose, when the caught error is logged in the same catch.
    skip handler.descendants('call_expression').any(logCall)

    report n, "The caught error is not used by the new one, so the original cause is lost: pass { cause: e }, or include e.message"
}
