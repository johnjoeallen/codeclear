// generated-from: review/common/sensitive-data-in-log/rule.md
// rule-hash: 4136725a5f572821c4e056020fad96a61f35507c5f2fe05d14df17d1f21ffed1
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'sensitive-data-in-log'

def secret = { i -> i ==~ /(?i)(\w*[A-Z_]|^)?(password|passwd|pwd|secret|token|accesstoken|refreshtoken|apikey|authorization|cookie|ssn|cardnumber|cvv|iban|creditcard)\w*/ }
def wholeObject = { i -> i in ['request', 'req', 'httpRequest', 'headers', 'httpHeaders', 'user', 'principal', 'credentials'] }

// Whether a value is really sensitive depends on what it holds, so this is a candidate for the AI.
Call.each { x ->
    only (x.name in ['trace', 'debug', 'info', 'warn', 'error', 'fatal', 'log'] && x.scope.text ==~ /(?i)(this\.)?(log|logger|LOG|LOGGER)/)
        || (x.name in ['println', 'print', 'printf'] && x.scope.text in ['System.out', 'System.err'])
    skip file.isTest
    // "Do not flag": a message that only names the word: a string literal has no identifiers. A masking helper hides the value.
    def masked = { a -> a.calls.any { k -> k.name ==~ /(?i)(mask|redact|obfuscate|hide|sanitize|anonymi[sz]e)\w*/ } }

    candidate x, "A log call includes a value that looks sensitive (a password, token, key or whole request): mask it or leave it out, because logs are kept and widely readable",
        x.args.any { a -> !masked(a) && a.identifiers.any(secret) }
    candidate x, "A log call includes a whole request, headers or user object, which can carry credentials and personal data: log only the fields that are needed",
        x.args.any { a -> a instanceof Name && wholeObject(a.name) && !(a.declaredType ==~ /(String|CharSequence|int|long|Integer|Long|UUID|boolean)/) }
}
