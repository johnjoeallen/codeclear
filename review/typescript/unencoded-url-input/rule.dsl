// generated-from: review/typescript/unencoded-url-input/rule.md
// rule-hash: 047b5fe1535086e7dbbc60c463c21991db5dc1eeaa05c53469ee90acc4f13e0b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unencoded-url-input'

def encoded = { s -> s.text ==~ /(?s)\$\{\s*(encodeURIComponent|encodeURI|escape)\(.*/ }
def numeric = { s -> s.text ==~ /(?is)\$\{\s*(\w+\.)*(page|limit|offset|count|size|index|year|month|day|port|total|n|i)\s*\}/ }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    only fn in ['fetch', 'window.open', 'open', 'got', 'request'] || fn ==~ /axios(\.(get|post|put|patch|delete|head|request))?/
    def a0 = c.arg(0)
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    // A template literal or concatenation that forms the URL.
    only a0 != null && ((a0.type == 'template_string' && !a0.descendants('template_substitution').isEmpty()) || (a0.type == 'binary_expression' && '+' in a0.tokens && !a0.descendants('identifier').isEmpty()))
    // "Do not flag": a substitution wrapped in encodeURIComponent, a numeric one, and a URL built with URLSearchParams.
    skip f.text ==~ /(?s).*URLSearchParams.*/ && a0.type != 'template_string'

    candidate c, "A value from the user (a parameter, the request, location, props) is put into the URL without encodeURIComponent: a value such as 'a&admin=true' or '../x' changes the path or adds query parameters. Wrap it in encodeURIComponent, or build the query with URLSearchParams",
        (a0.type == 'template_string' && a0.descendants('template_substitution').any { s -> !encoded(s) && !numeric(s) && (s.usesParameterOf(f) || s.mentions('req') || s.mentions('location') || s.mentions('searchParams') || s.mentions('props') || s.mentions('params') || s.mentions('query')) })
            || (a0.type == 'binary_expression' && !(a0.text ==~ /(?s).*(encodeURIComponent|encodeURI).*/) && (a0.usesParameterOf(f) || a0.mentions('req') || a0.mentions('location') || a0.mentions('searchParams') || a0.mentions('props') || a0.mentions('query')))
}
