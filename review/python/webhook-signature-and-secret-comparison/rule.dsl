// generated-from: review/python/webhook-signature-and-secret-comparison/rule.md
// rule-hash: 92b7c3f3231f240993da34000b678e163f85eff55f4d2403aa4730eaa6846204
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'webhook-signature-and-secret-comparison'

def secretName = { n -> n ==~ /(?i)(\w*[_.])?(signature|sig|token|secret|api_?key|hmac|hexdigest|digest|expected|computed|mac)\w*/ && !(n ==~ /(?i).*(count|length|type|url|file|name|id|header)$/) }
// An operand that is a secret-looking name, an attribute with such a name, a hexdigest() or digest() result, or a header lookup.
def secretOperand = { o -> (o.type == 'identifier' && secretName(o.text)) || (o.type == 'attribute' && secretName(o.field('attribute').text))
    || (o.type == 'call' && o.field('function').type == 'attribute' && o.field('function').field('attribute').text in ['hexdigest', 'digest', 'b64encode'])
    || (o.type == 'subscript' && o.text ==~ /(?is).*(signature|x-hub|token|secret).*/) }
def plain = { o -> o.type in ['none', 'integer', 'float', 'true', 'false'] || (o.type == 'string' && o.descendants('interpolation').isEmpty() && o.text ==~ /["']{1,3}["']{1,3}/) }

// A comparison that stops at the first different character leaks the secret through timing, so hmac.compare_digest is the right tool.
comparison_operator.each { c ->
    only '==' in c.tokens || '!=' in c.tokens
    skip file.isTest
    def operands = c.children
    only operands.any(secretOperand)
    // "Do not flag": a comparison with None or a literal says nothing about a secret.
    skip operands.any(plain)

    candidate c, "A signature or secret is compared with ==: the comparison stops at the first different character, which leaks the secret through timing. Use hmac.compare_digest()"
}
