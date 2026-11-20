// generated-from: review/java/transactional-on-private-method/rule.md
// rule-hash: 296cdf25c2b607b8716cddb194ecefcdd7b620dafefd892fe6ab87508d34d5b6
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'transactional-on-private-method'

def isTransactional = { a -> a == 'Transactional' }

// A call from inside the class to a @Transactional method of the same class does not go through the proxy.
Call.each { c ->
    def target = c.sameClassTransactionalTarget
    only target != null
    skip project.config('transaction.mode') == 'aspectj'

    // "Do not flag": a caller that is itself transactional (method or class): a transaction already exists.
    def caller = c.enclosingMethod
    skip (caller != null && caller.annotations.any(isTransactional)) || c.enclosingClassAnnotations.any(isTransactional)

    report c, "Calling ${target.name}() from inside the same class bypasses the Spring proxy, so its @Transactional has no effect",
        confidence: MEDIUM
}
