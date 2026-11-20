// generated-from: review/java/optional-get-without-check/rule.md
// rule-hash: ac03f73e34cc54e7e1cdb68198f070596873e865ecfd624ece106c661dfe0cd9
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
