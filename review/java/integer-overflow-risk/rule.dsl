// generated-from: review/java/integer-overflow-risk/rule.md
// rule-hash: ae395a4d029b607618d9b44aa1df7ecdf1726a813b995cbce7fc2d9f685ccd81
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'integer-overflow-risk'

def factor = { s -> s ==~ /(1000|60|24|365|3600|86400|1024|1_000|1_000_000|1000000)/ }

// Whether the real values reach the limit is a question of data, so this is a candidate for the AI.
Binary.each { b ->
    only b.operator == '*'
    skip file.isTest
    // Only the whole product: a nested multiplication is part of its parent.
    skip b.parent instanceof Binary && b.parent.operator == '*'
    // A time or size factor, and a name in the product (a product of literals is a constant that the compiler checks).
    only b.text ==~ /(?s).*\b(1000|60|24|365|3600|1024)\b.*/ && b.text ==~ /(?s).*[A-Za-z_].*/
    // "Do not flag": a long operand or a long literal (1000L, 24L, (long) x, TimeUnit).
    skip b.text ==~ /(?s).*(\b\d[\d_]*[lL]\b|\(\s*long\s*\)|TimeUnit|Duration|\.toMillis|\.toSeconds|Math\.multiplyExact).*/

    candidate b, "This int multiplication by a time or size factor can overflow past 2^31-1 and wrap to a negative number: make one operand long (24L) before multiplying"
}
