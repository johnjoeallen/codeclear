// generated-from: review/java/bigdecimal-misuse/rule.md
// rule-hash: 5b2e60aeaac58e1e8699b5a044408921e0266efa5711a275d1ebd71a6adb9394
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'bigdecimal-misuse'

// new BigDecimal(double or float) carries the binary rounding error into the BigDecimal.
// "Do not flag": a String, BigDecimal.valueOf(double), and an int or long: none of them has a double or float type.
New.each { n ->
    only n.typeName == 'BigDecimal'

    report n, "new BigDecimal(${n.firstArg.text}) takes a binary floating-point value and keeps its rounding error; use BigDecimal.valueOf(...) or a String",
        n.firstArg instanceof DoubleLit || n.firstArg.type in ['double', 'float']
}
