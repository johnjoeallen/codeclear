// generated-from: review/typescript/loose-equality/rule.md
// rule-hash: 5b1b9778e09272eee922b1bb2f9252e11891de76745ec51a5fa0dbaff997f164
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'loose-equality'

binary_expression.each { b ->
    only '==' in b.tokens || '!=' in b.tokens
    def l = b.field('left')
    def r = b.field('right')
    // "Do not flag": a comparison with null, which covers undefined on purpose ...
    skip l.type == 'null' || r.type == 'null'
    // ... and operands of the same known primitive type: two literals, or typeof x against a string.
    skip l.type == r.type && l.type in ['string', 'number', 'true', 'false']
    skip l.type == 'unary_expression' && 'typeof' in l.tokens && r.type == 'string'

    report b, "== and != convert their operands before comparing ('1' == 1, 0 == ''): use === or !=="
}
