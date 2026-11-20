// generated-from: review/java/transactional-rollback-mismatch/rule.md
// rule-hash: baa75129da53e6d5871a4fae6520a377898b4d3060933529b899196f2c5ce781
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'transactional-rollback-mismatch'

def checked = { t -> t in ['Exception', 'IOException', 'SQLException', 'FileNotFoundException', 'TimeoutException', 'InterruptedException', 'ExecutionException', 'GeneralSecurityException', 'URISyntaxException'] }
def writeName = { n -> n in ['save', 'saveAll', 'saveAndFlush', 'persist', 'merge', 'delete', 'deleteById', 'deleteAll', 'update', 'batchUpdate', 'insert', 'flush', 'executeUpdate'] }

// Whether the partial commit is the intended outcome needs intent, so this is a candidate for the AI.
Method.each { m ->
    only 'Transactional' in m.annotations || 'Transactional' in m.enclosingClassAnnotations
    skip file.isTest

    // "Do not flag": a rollbackFor or noRollbackFor, and a catch that marks the transaction rollback-only.
    skip m.text ==~ /(?s).*@Transactional\s*\([^)]*(rollbackFor|noRollbackFor).*/
    skip m.text ==~ /(?s).*setRollbackOnly.*/

    candidate m.nameNode, "${m.name}() throws a checked exception but @Transactional has no rollbackFor: the transaction commits although the method fails",
        m.throwsTypes.any(checked)
    // A catch that neither rethrows nor leaves the method, in a method that writes.
    candidate m.nameNode, "${m.name}() writes and then swallows a failure in a catch block, so the transaction commits the partial work",
        m.calls.any { x -> writeName(x.name) } && m.catches.any { c -> c.body.throws.isEmpty() && c.param.type != null }
    // A catch that turns an unchecked failure into a checked one: the rollback rule does not apply to it.
    candidate m.nameNode, "${m.name}() wraps a failure in a checked exception inside a transaction without rollbackFor",
        m.catches.any { c -> c.body.throws.any { t -> t.expression instanceof New && checked(t.expression.typeName) } }
}
