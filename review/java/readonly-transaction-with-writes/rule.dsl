// generated-from: review/java/readonly-transaction-with-writes/rule.md
// rule-hash: c90a3dd2b417ceaf2caaca890abf32c7bbd702a7c7b770512e8b0a0ff25b7a46
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'readonly-transaction-with-writes'

// Writes are recognised by the type of the receiver, so a cache or a metrics registry with a save() or flush() is not one.
def store = { t -> t ==~ /.*(Repository|EntityManager|Dao|DAO)/ }
def jdbc = { t -> t ==~ /.*Jdbc.*/ }

Call.each { c ->
    only c.inReadOnlyTransaction
    skip file.isTest

    candidate c, "${c.name}() writes inside a readOnly transaction: the write may be skipped or rejected",
        c.name in ['save', 'saveAll', 'saveAndFlush', 'delete', 'deleteById', 'deleteAll', 'deleteAllById', 'deleteInBatch', 'persist', 'merge', 'flush', 'remove']
            && store(c.scope.declaredType)
    candidate c, "${c.name}() writes inside a readOnly transaction: the write may be skipped or rejected",
        c.name in ['update', 'batchUpdate'] && jdbc(c.scope.declaredType)
}
