// generated-from: review/typescript/transaction-client-not-used/rule.md
// rule-hash: b830cd6142f48de78fdab0765f64b978273dc72213475bbb3e9de0283b3569e9
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'transaction-client-not-used'

def txCall = { t -> t ==~ /(\w+\.)*(\$transaction|transaction|withTransaction|runInTransaction)/ }
def outer = { r -> r in ['prisma', 'db', 'knex', 'dataSource', 'sequelize', 'connection', 'pool', 'em', 'entityManager'] || r ==~ /(?i)\w*(repo|repository)/ || r ==~ /[A-Z]\w*/ }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function')
    only fn.type == 'member_expression'
    // The transaction call around this one, and the handle its callback receives.
    only c.ancestors('call_expression').any { k -> txCall(k.field('function').text) && k.args.any { a -> a.type in ['arrow_function', 'function_expression'] } }
    def obj = fn.field('object')
    def root = obj.type == 'identifier' ? obj.text : (obj.type == 'member_expression' && obj.field('object').type == 'identifier' ? obj.field('object').text
        : (obj.type == 'call_expression' && obj.field('function').type == 'identifier' ? obj.field('function').text : ''))
    only outer(root)
    // "Do not flag": the transaction handle passed in the arguments (Sequelize { transaction: t }, Mongo { session }).
    skip c.args.any { a -> a.text ==~ /(?s).*\b(transaction|session|trx|tx|manager|t)\b.*/ }
    // "Do not flag": the call is itself the transaction call.
    skip txCall(fn.text)

    candidate c, "${fn.text}() runs on the outer connection, not on the transaction's own handle: it is outside the transaction, so it is not rolled back with it and cannot see its uncommitted rows. Use the tx, trx or manager the callback receives",
        true
}
