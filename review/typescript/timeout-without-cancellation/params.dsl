// generated-from: review/typescript/timeout-without-cancellation/rule.md
// rule-hash: 7ec799b41ea1128b32c7a3985f07b9773475364003ac45ebdc64a596d641d171
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'timeout-without-cancellation'

// A timeout or deadline that is taken as a parameter and then never used: callers believe the call is bounded.
Node.each { f ->
    only f.type in ['function_declaration', 'arrow_function', 'function_expression', 'method_definition']
    skip file.isTest
    def p = f.field('parameters')
    only p != null

    candidate f, "This function takes a timeout or deadline and never passes it on, so callers think the call is bounded when it is not: forward it to the client call (signal: AbortSignal.timeout(ms))",
        (p.mentions('timeout') && f.countOf('timeout').isEqualTo(1)) || (p.mentions('timeoutMs') && f.countOf('timeoutMs').isEqualTo(1)) || (p.mentions('deadline') && f.countOf('deadline').isEqualTo(1))
}
