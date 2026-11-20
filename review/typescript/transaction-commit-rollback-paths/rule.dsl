// generated-from: review/typescript/transaction-commit-rollback-paths/rule.md
// rule-hash: cb88559fd56e9e422766adc25c92a34315f579926a4bc255aeb447adada982db
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'transaction-commit-rollback-paths'

def txCall = { t -> t ==~ /(\w+\.)*(\$transaction|transaction|withTransaction|runInTransaction)/ }

Node.each { n ->
    only n.type in ['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'try_statement', 'expression_statement']
    skip file.isTest
    def isFunction = n.type in ['function_declaration', 'arrow_function', 'function_expression', 'method_definition']
    def text = n.text
    def release = isFunction && n.descendants('finally_clause').any { f -> f.mentions('release') || f.mentions('end') || f.mentions('close') || f.mentions('destroy') }
    def begins = isFunction && text ==~ /(?s).*(query\(\s*["'`]BEGIN|beginTransaction\(|startTransaction\(|\.begin\().*/
    def acquires = isFunction && text ==~ /(?s).*(pool\.connect\(|getConnection\(|\.connect\(\)).*/
    def c = n.child('call_expression')

    candidate n, "This function starts a transaction and has no ROLLBACK, so when something throws the transaction stays open, holding its locks, until the connection is closed",
        begins && !(text ==~ /(?is).*rollback.*/)
    candidate n, "This function takes a connection and does not release it in a finally block: when something throws the connection is never returned, and the pool runs dry",
        acquires && !release
    // A try/catch inside a transaction callback that swallows the error: the transaction then commits what it did.
    candidate n, "This catch is inside a transaction callback and does not rethrow, so the transaction commits as if nothing had failed: rethrow so that the transaction rolls back",
        n.type == 'try_statement' && n.child('catch_clause') != null && n.child('catch_clause').descendants('throw_statement').isEmpty()
            && n.ancestors('call_expression').any { k -> txCall(k.field('function').text) && k.args.any { a -> a.type in ['arrow_function', 'function_expression'] } }
    candidate n, "commit() is not awaited, so the code goes on before the commit finishes and a failure to commit is an unhandled rejection",
        c != null && c.field('function').type == 'member_expression' && c.field('function').field('property').text == 'commit'
}
