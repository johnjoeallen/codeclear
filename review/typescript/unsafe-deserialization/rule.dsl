// generated-from: review/typescript/unsafe-deserialization/rule.md
// rule-hash: 93b7ff953a8a1bbe6e3c8f279915927280ff05f114eb5a2c2c2660a306bf9a0c
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unsafe-deserialization'

def literal = { a -> a != null && (a.type == 'string' || (a.type == 'template_string' && a.descendants('template_substitution').isEmpty())) }

Node.each { n ->
    only n.type in ['call_expression', 'new_expression']
    skip file.isTest
    def fn = n.type == 'call_expression' ? n.field('function').text : ''
    def f = n.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    def a0 = n.arg(0)
    // Data that came from outside: the request, a queue message, a cookie, an upload.
    def external = a0 != null && !literal(a0) && (a0.derivedFrom('req', f) || a0.derivedFrom('request', f) || a0.derivedFrom('message', f) || a0.derivedFrom('body', f) || a0.derivedFrom('cookies', f) || a0.derivedFrom('event', f) || a0.derivedFrom('file', f) || a0.derivedFrom('buffer', f))

    // "Do not flag": JSON.parse, yaml.safeLoad and the default (safe) schema of js-yaml 4, none of which are matched.
    candidate n, "${fn}() rebuilds objects, functions or code from the data it is given, so data from outside can run code on the server: use JSON.parse, or a format that carries only data",
        fn ==~ /(\w+\.)*(unserialize|deserialize)/ && external
    candidate n, "yaml.load() with the full schema builds arbitrary objects, functions included: use the default safe schema (js-yaml 4), or safeLoad",
        fn ==~ /(\w+\.)*yaml\.load|jsyaml\.load|YAML\.load/ && n.args.any { a -> a.text ==~ /(?s).*(FULL_SCHEMA|UNSAFE_SCHEMA|DEFAULT_FULL_SCHEMA).*/ }
    candidate n, "eval() runs the string it is given as code: with data from outside, whoever controls the data controls the server. Use JSON.parse",
        fn == 'eval' && external
    candidate n, "new Function() compiles the string it is given as code: with data from outside, whoever controls the data controls the server",
        n.type == 'new_expression' && n.field('constructor').text == 'Function' && !literal(a0) && (n.args.any { a -> a.derivedFrom('req', f) || a.derivedFrom('request', f) || a.derivedFrom('message', f) || a.derivedFrom('body', f) })
}
