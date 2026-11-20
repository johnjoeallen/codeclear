// generated-from: review/typescript/remote-call-in-transaction/rule.md
// rule-hash: 93cfc6801d90894435638d015cf407200e37faf8accccc80b4a5a9a4132b11ca
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'remote-call-in-transaction'

def remote = { t -> t in ['fetch', 'sendMail', 'setTimeout'] || t ==~ /(axios|got|request|superagent)(\.(get|post|put|patch|delete|head|request))?/ || t ==~ /https?\.(request|get)/
    || t ==~ /(\w+\.)*\w*(producer|sqs|sns|channel|publisher|mailer|transporter|kafka|eventbridge)\w*\.(send|publish|produce|sendToQueue|sendMail|sendBatch)/ || t ==~ /(\w+\.)*sendMail/ }
def transaction = { t -> t ==~ /(\w+\.)*(transaction|\$transaction|withTransaction|runInTransaction|startTransaction|inTransaction)/ }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    only remote(fn)
    // "Do not flag": work handed to afterCommit, onCommit or an outbox write.
    skip c.ancestors('call_expression').any { k -> k.field('function').text ==~ /(\w+\.)*(afterCommit|onCommit|after_commit|outbox|addOutbox)/ }
    // Inside the callback of db.transaction(...), prisma.$transaction(...), knex.transaction(...) and similar.
    def inTransaction = c.ancestors('call_expression').any { t -> transaction(t.field('function').text) && t.args.any { a -> a.type in ['arrow_function', 'function_expression'] } }

    candidate c, "${fn}() is a network call or a wait inside a database transaction: it holds locks and a pooled connection for as long as it takes, and when it fails after a write the data and the outside world disagree. Do it after the commit, or write an outbox row",
        inTransaction
}
