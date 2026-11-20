// generated-from: review/java/unconsumed-lazy-stream/rule.md
// rule-hash: 2322a96c3405ecc2132684d4b51a528c2029332d7e79b0d0e05954d2e20283a8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'unconsumed-lazy-stream'

def isIntermediate = { n -> n in ['map', 'filter', 'peek', 'sorted', 'flatMap', 'distinct', 'limit', 'skip', 'mapToInt', 'mapToLong', 'mapToDouble', 'mapToObj', 'boxed', 'takeWhile', 'dropWhile'] }

// A statement of its own that ends in an intermediate operation: nothing consumes the stream, so no element is processed.
// "Do not flag": Optional and reactive chains do not start with stream(), so they are not matched.
Call.each { c ->
    only c.isDiscarded && isIntermediate(c.name) && c.chainStartsStream
    skip file.isTest

    report c, "The stream ends in ${c.name}(), an intermediate operation, and nothing consumes it: none of its elements is processed"
}
