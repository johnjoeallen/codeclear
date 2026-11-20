// generated-from: review/typescript/allsettled-ignored-rejections/rule.md
// rule-hash: a880d90b92d0a09e2c3e9644f3b971022e878c647bf27280fe3d3d3fb4e68d27
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'allsettled-ignored-rejections'

def functionTypes = ['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'generator_function_declaration', 'program']

call_expression.each { c ->
    only c.field('function').text == 'Promise.allSettled'
    skip file.isTest
    // "Do not flag": cleanup code, where a failure can be ignored.
    def fn = c.ancestorOf(functionTypes)
    // An arrow function or function expression takes the name of the variable it is assigned to.
    def named = fn != null && fn.field('name') != null ? fn.field('name') : (fn != null && fn.parent != null && fn.parent.type == 'variable_declarator' ? fn.parent.field('name') : fn)
    skip named != null && named.type == 'identifier' && named.text ==~ /(?i).*(cleanup|dispose|teardown|shutdown).*/

    // The call, with the await in front of it if there is one.
    def outer = c.parent != null && c.parent.type == 'await_expression' ? c.parent : c
    def holder = outer.parent
    def scope = c.ancestorOf(functionTypes)
    def v = holder != null && holder.type == 'variable_declarator' ? holder.field('name') : holder
    // A result that is neither returned nor passed on is read through .status or .reason, or not at all.
    report c, "The result of Promise.allSettled() is thrown away, so a rejected promise goes unnoticed: check each result's status",
        holder != null && holder.type == 'expression_statement'
    report c, "The result of Promise.allSettled() is never read for status or reason, so a rejected promise goes unnoticed",
        holder != null && holder.type == 'variable_declarator' && v.type == 'identifier'
            && (scope.countOf(v.text).isEqualTo(1) || !(scope.mentions('status') || scope.mentions('reason')))
}
