// generated-from: review/java/unconsumed-lazy-stream/rule.md
// rule-hash: 2322a96c3405ecc2132684d4b51a528c2029332d7e79b0d0e05954d2e20283a8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'unconsumed-lazy-stream'

// A Stream stored in a local variable that nothing uses again.
Var.each { v ->
    only v.typeName in ['Stream', 'IntStream', 'LongStream', 'DoubleStream'] && v.neverReferenced
    skip file.isTest

    report v, "The stream ${v.name} is created and never used: nothing consumes it"
}
