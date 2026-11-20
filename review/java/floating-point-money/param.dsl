// generated-from: review/java/floating-point-money/rule.md
// rule-hash: 4d6dd28fd3db2a62e3c593afcc0f55f2c2d2aa6ba48d785da3067cb02210a910
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'floating-point-money'

Param.each { p ->
    only p.type in ['double', 'float', 'Double', 'Float'] && p.name ==~ /(?i).*(amount|price|cost|balance|total|fee|tax|salary|payment|money|currency).*/
    skip p.name ==~ /(?i).*(rate|percent|ratio|score|metric|count).*/ || file.isTest

    candidate p, "The parameter ${p.name} looks like an amount of money but is a ${p.type} used in arithmetic: use BigDecimal or an integer number of cents",
        p.usedInArithmetic
}
