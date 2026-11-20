// generated-from: review/typescript/non-constant-time-secret-compare/rule.md
// rule-hash: e340040c6ff285e97a25d6bbf917c4176eac4448766f2161ec347f6d23a335e5
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'non-constant-time-secret-compare'

// A name that ends in a secret word: signature, sig, hmac, digest, apiKey, token, secret, hash. tokenId and hashing do not.
def secretName = { t -> t ==~ /(?is)["'`]?(.*[-_.])?\w*(signature|sig|hmac|digest|api[-_]?key|token|secret|hash)s?["'`]?/ }
def nameOf = { o -> o.type == 'identifier' ? o.text : o.type == 'member_expression' ? o.field('property').text
    : o.type == 'subscript_expression' ? o.field('index').text : '' }
def secretOperand = { o -> secretName(nameOf(o)) || (o.type == 'call_expression' && o.text ==~ /(?s).*createHmac.*digest.*/) }
// "Do not flag": a comparison with a literal, null, undefined or an empty string says nothing about a secret.
def literal = { o -> o.type in ['null', 'undefined', 'number', 'true', 'false', 'string', 'template_string', 'unary_expression'] }

binary_expression.each { b ->
    only '==' in b.tokens || '===' in b.tokens || '!=' in b.tokens || '!==' in b.tokens
    skip file.isTest
    def l = b.field('left')
    def r = b.field('right')

    report b, "A secret is compared with an equality operator: the comparison stops at the first different byte, which leaks the secret through timing. Use crypto.timingSafeEqual()",
        (secretOperand(l) && !literal(r)) || (secretOperand(r) && !literal(l))
}
