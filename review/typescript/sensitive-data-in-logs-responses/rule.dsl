// generated-from: review/typescript/sensitive-data-in-logs-responses/rule.md
// rule-hash: fb7d0160784d07d5e890b40058ecf827fd6a4a8e4a156f17fa2b59aec5ff48c3
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'sensitive-data-in-logs-responses'

def secret = { n -> n ==~ /(?i)(\w*[_.])?(password|passwd|pwd|secret|token|accesstoken|refreshtoken|apikey|api_key|authorization|cookie|ssn|cardnumber|card|cvv|creditcard|privatekey)\w*/ && !(n ==~ /(?i).*(count|length|type|url|name|id)$/) }
def whole = { t -> t ==~ /(?s)\s*(req|request)(\.(headers|body|cookies|query|params))?\s*/ }
def masked = { a -> a.text ==~ /(?s).*(mask|redact|sanitize|obfuscate|omit|pick\().*/ }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    // "Do not flag": a debug value shown only outside production (a NODE_ENV check around the call).
    skip c.ancestors('if_statement').any { i -> i.field('condition').text ==~ /(?s).*NODE_ENV.*/ }
    skip c.ancestors('ternary_expression').any { t -> t.text ==~ /(?s).*NODE_ENV.*/ }

    candidate c, "A log call includes a value that looks sensitive (a password, token, key, card, authorization header or cookie): mask it or leave it out, because logs are kept and widely readable",
        fn ==~ /(console|logger|log|this\.logger|this\.log)\.(log|debug|info|warn|error|trace|fatal)/
            && c.args.any { a -> !masked(a) && (a.descendants('identifier').any { i -> secret(i.text) } || a.descendants('property_identifier').any { i -> secret(i.text) } || (a.type == 'identifier' && secret(a.text))) }
    candidate c, "A log call includes the whole request, its headers or body, or a user object, which carry credentials and personal data: log only the fields that are needed",
        fn ==~ /(console|logger|log|this\.logger|this\.log)\.(log|debug|info|warn|error|trace|fatal)/
            && c.args.any { a -> !masked(a) && (whole(a.text) || (a.type == 'identifier' && a.text in ['user', 'account', 'customer'] && f.field('parameters') != null && !(f.field('parameters').text ==~ /(?s).*\b(user|account|customer)\s*:\s*(string|number|boolean)\b.*/) && f.field('parameters').mentions(a.text))) }
    // A response that carries a stack trace or the text of a driver error.
    candidate c, "The response carries a stack trace or the raw error, which shows internals (paths, queries, library versions) to the caller: log it and send a generic message",
        fn ==~ /(\w+\.)?(res|response|reply)\.(send|json|end|status\(\d+\)\.(send|json))|(res|response)\.(send|json)/ && c.args.any { a -> a.text ==~ /(?s).*\b(err|error|e|ex)\.(stack|message)\b.*/ || (a.type == 'identifier' && a.text in ['err', 'error', 'e', 'ex']) || (a.type == 'object' && a.children.any { p -> (p.type == 'pair' && p.field('value').type == 'identifier' && p.field('value').text in ['err', 'error', 'e']) || (p.type == 'shorthand_property_identifier' && p.text in ['err', 'error', 'e']) }) }
}
