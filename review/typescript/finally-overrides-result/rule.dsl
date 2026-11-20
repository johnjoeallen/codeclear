// generated-from: review/typescript/finally-overrides-result/rule.md
// rule-hash: c080d6e7ef9012ad89abf544b5059f2ec5effa1439c602a0b172425b7a34466b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'finally-overrides-result'

def jump = { n -> n.type in ['return_statement', 'throw_statement', 'break_statement', 'continue_statement'] }
def boundary = ['finally_clause', 'function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'generator_function_declaration',
                'for_statement', 'for_in_statement', 'while_statement', 'do_statement']
def cleanupName = { n -> n in ['close', 'release', 'rollback', 'end', 'disconnect'] }

Node.each { n ->
    skip file.isTest
    // A return, throw, break or continue that leaves a finally block replaces whatever the try block was going to do.
    // A break or continue whose loop is inside the finally stops at that loop; a switch ends a break, but not a continue.
    def nearest = n.ancestorOf(boundary)
    def f = nearest != null && nearest.type == 'finally_clause' ? nearest : n.ancestor('finally_clause')
    // A throw that a try inside the finally catches never leaves it.
    def caught = f != null && f.descendants('try_statement').any { t -> t.field('handler') != null && t.field('body').descendants('throw_statement').any { x -> x == n } }

    // Cleanup in a finally that can itself throw, when the try's catch rethrows: the cleanup error hides the original one.
    def block = f.parent
    def nested = f != null && f.descendants('try_statement').any { t -> t.field('body').descendants('call_expression').any { k -> k == n } }

    report n, "This ${n.type} leaves a finally block, so it replaces the result or the error that the try block was producing",
        jump(n) && nearest != null && nearest.type == 'finally_clause' && !caught
    report n, "${n.field('function').field('property').text}() in a finally can throw, and it is not wrapped in its own try/catch: its error replaces the original one that the catch rethrows",
        n.type == 'call_expression' && n.field('function').type == 'member_expression' && cleanupName(n.field('function').field('property').text)
            && f != null && nearest != null && nearest.type == 'finally_clause' && !nested
            && block.field('handler') != null && !block.field('handler').descendants('throw_statement').isEmpty()
}
