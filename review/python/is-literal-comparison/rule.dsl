// generated-from: review/python/is-literal-comparison/rule.md
// rule-hash: 813ce963bf5bc09d778f3bf7c38083c8e3326c509af3e7c751cf26bbeb0907e9
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'is-literal-comparison'

def literal = { v -> v.type in ['string', 'concatenated_string', 'integer', 'float', 'tuple'] }

comparison_operator.each { c ->
    only c.tokens.any { t -> t == 'is' || t == 'is not' }
    skip file.isTest
    // "Do not flag": None, True, False and Ellipsis are compared with is on purpose; those are not literals here.
    only c.children.any(literal)

    report c, "'is' compares identity, not value: a literal here works by accident of the interpreter, so use == or !="
}
