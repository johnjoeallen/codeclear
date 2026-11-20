// generated-from: review/java/floating-point-money/rule.md
// rule-hash: 4d6dd28fd3db2a62e3c593afcc0f55f2c2d2aa6ba48d785da3067cb02210a910
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'floating-point-money'

// A variable that holds money by its name, is a double or float, and is used in arithmetic. Whether it is displayed only is for the AI.
Var.each { v ->
    only v.typeName in ['double', 'float', 'Double', 'Float'] && v.name ==~ /(?i).*(amount|price|cost|balance|total|fee|tax|salary|payment|money|currency).*/
    // "Do not flag": names that denote ratios, metrics or scores.
    skip v.name ==~ /(?i).*(rate|percent|ratio|score|metric|count).*/ || file.isTest

    candidate v, "${v.name} looks like an amount of money but is a ${v.typeName} used in arithmetic: use BigDecimal or an integer number of cents",
        v.usedInArithmetic
}
