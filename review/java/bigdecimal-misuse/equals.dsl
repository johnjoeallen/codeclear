// generated-from: review/java/bigdecimal-misuse/rule.md
// rule-hash: 5b2e60aeaac58e1e8699b5a044408921e0266efa5711a275d1ebd71a6adb9394
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'bigdecimal-misuse'

// BigDecimal.equals compares the scale as well as the value.
Call.each { c ->
    only c.name == 'equals' && c.scope.type == 'java.math.BigDecimal'
    // "Do not flag": inside an equals() implementation that intentionally compares scale.
    skip c.enclosingMethodName == 'equals'

    report c.nameNode, "BigDecimal.equals also compares the scale (2.0 does not equal 2.00); use compareTo(...) == 0"
}
