// generated-from: review/java/optional-get-without-check/rule.md
// rule-hash: ea620cfc905c1a71ba198b8d84c700bd3eb21b752058179ae336fb183e9eb249
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'optional-get-without-check'

Call.each { c ->
    only c.name in ['get', 'getAsInt', 'getAsLong', 'getAsDouble']
        && c.args.isEmpty()
        && c.scope != null
    skip file.isTest

    def recv = c.scope
    def resolved = recv.type != null
    def guess = (recv instanceof Call && recv.name in ['findById', 'findFirst', 'findAny', 'findOne',
                                                       'max', 'min', 'reduce', 'findFirstBy', 'findOneBy'])
             || (recv instanceof Name && recv.declaredType ==~ /Optional(Int|Long|Double)?(<.*>)?/)

    // Type known: only Optional types count. Type unknown: fall back to names, at LOW confidence.
    only !resolved || recv.type in ['java.util.Optional', 'java.util.OptionalInt',
                                    'java.util.OptionalLong', 'java.util.OptionalDouble']
    only resolved || guess

    skip (recv instanceof Name || recv instanceof Field) && c.guarded(recv, PRESENT)
    // A parameter that every caller in the project passes present (guarded there, or Optional.of) is guarded too.
    skip recv instanceof Name && c.guardedByCallers(recv, PRESENT)

    report c.nameNode,
        "${c.name}() on an Optional without a presence check; use orElseThrow(...) or guard it",
        confidence: resolved ? DEFAULT : LOW
}
