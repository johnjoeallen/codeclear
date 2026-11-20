// generated-from: review/python/sensitive-data-in-logs-and-errors/rule.md
// rule-hash: d701638527038538da0e680ef7782be83c06b80f78882dac352b0c767c3d591e
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'sensitive-data-in-logs-and-errors'

// A name that holds a secret or personal data; token_count and card_type do not.
def secret = { n -> n ==~ /(?i)(\w*[_.])?(password|passwd|secret|token|authorization|api_?key|ssn|card_?number|cvv|credit_?card|private_?key|session_?id)\w*/ && !(n ==~ /(?i).*(count|length|len|size|type|url|file|path|name|id_?field)$/) }
def whole = { t -> t ==~ /(?s)\s*(self\.)?request\.(headers|body|json|data|form|cookies|args)\s*/ }
def maskCall = { k -> k.field('function').text ==~ /(?i)(\w+\.)*(mask|redact|obfuscate|hide|sanitize|anonymi[sz]e)\w*/ }

Node.each { n ->
    only n.type in ['call', 'return_statement', 'assignment']
    skip file.isTest
    def fn = n.type == 'call' ? n.field('function').text : ''
    def handler = n.ancestorOf(['except_clause', 'function_definition', 'lambda'])

    // A logging call (or print) with a value that looks sensitive, or a whole request. A masked value is not looked at.
    candidate n, "A log call includes a value that looks sensitive (a password, token, key or card): mask it or leave it out, because logs are kept and widely readable",
        fn ==~ /(self\.)?(logger|logging|log|LOG)\.(debug|info|warning|warn|error|exception|critical|log)|print/
            && n.args.any { a -> !(a.type == 'call' && (maskCall(a) || a.field('function').text in ['len', 'bool', 'type'])) && !a.descendants('call').any(maskCall)
            && (a.descendants('identifier').any { i -> secret(i.text) } || (a.type == 'identifier' && secret(a.text))) }
    candidate n, "A log call includes a whole request, its headers, body or cookies, which carry credentials and personal data: log only the fields that are needed",
        fn ==~ /(self\.)?(logger|logging|log|LOG)\.(debug|info|warning|warn|error|exception|critical|log)|print/ && n.args.any { a -> whole(a.text) || a.descendants('attribute').any { t -> whole(t.text) } }
    // An error handler that sends the exception text or the stack trace back to the caller.
    candidate n, "The response carries the exception text or stack trace, which shows internals (paths, queries, library versions) to the caller: log it and return a generic message",
        n.type == 'return_statement' && handler != null && handler.type == 'except_clause'
            && n.text ==~ /(?s).*(traceback\.format_exc\(\)|format_exc\(\)|\bstr\(\s*\w+\s*\)|repr\(\s*\w+\s*\)).*/
    // Debug mode shows an interactive debugger and every variable to whoever reaches the error page.
    candidate n, "Debug mode is on: an error page shows source code, variables and an interactive console. Turn it off outside development",
        n.type == 'assignment' && n.field('left').text ==~ /(\w+\.)?DEBUG/ && n.field('right').type == 'true' && !(file.path ==~ /(?i).*(dev|local|test)[^\/]*\.py/)
    candidate n, "app.run(debug=True) shows an interactive debugger to whoever reaches an error page: turn it off outside development",
        fn ==~ /(\w+\.)?run/ && n.keyword('debug') != null && n.keyword('debug').type == 'true'
}
