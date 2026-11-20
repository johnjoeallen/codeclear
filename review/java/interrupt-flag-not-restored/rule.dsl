// generated-from: review/java/interrupt-flag-not-restored/rule.md
// rule-hash: 451535d46a71b69a31872557ddc4cb946bff52b5e028a80a8b48b410752c9f85
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'interrupt-flag-not-restored'

def restoresFlag = { x -> x.name == 'interrupt' && x.scope.text ==~ /Thread\.currentThread\(\)/ }

Catch.each { c ->
    // A catch of InterruptedException, alone or in a multi-catch.
    only c.param.type ==~ /.*\bInterruptedException\b.*/
    // "Do not flag": main methods and tests.
    skip file.isTest || c.enclosingMethodName == 'main'

    // The block restores the flag, or rethrows (a throw, including one that wraps the exception with its cause).
    def restores = c.body.calls.any(restoresFlag)
    // Deferred restore: the block only records the interruption (interrupted = true) and the method restores the
    // flag after the catch.
    def deferred = !c.body.assignments.isEmpty() && c.callsAfter.any(restoresFlag)
    skip restores || deferred || !c.body.throws.isEmpty()

    report c, "InterruptedException is caught but the interrupt flag is not restored; call Thread.currentThread().interrupt() or rethrow"
}
