// generated-from: review/java/unconsumed-lazy-stream/rule.md
// rule-hash: 2322a96c3405ecc2132684d4b51a528c2029332d7e79b0d0e05954d2e20283a8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'unconsumed-lazy-stream'

// These terminal operations can skip a map or peek stage (count() when the size is known, the others stop at the first match),
// so a side effect there does not always happen. A peek that only logs has no side effect in this sense.
Call.each { c ->
    only c.name in ['count', 'findFirst', 'findAny', 'anyMatch', 'allMatch', 'noneMatch'] && c.chainStartsStream
    skip file.isTest

    report c, "${c.name}() can skip the map or peek stage before it, so its side effect does not always run: do the side effect in forEach",
        c.hasSideEffectStage
}
