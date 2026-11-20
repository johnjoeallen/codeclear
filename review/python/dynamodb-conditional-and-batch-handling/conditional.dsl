// generated-from: review/python/dynamodb-conditional-and-batch-handling/rule.md
// rule-hash: e36e930c03b729b5c13ca105dc64c20527cab38de9e17df9734c7372bf330f46
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'dynamodb-conditional-and-batch-handling'

// A conditional write fails on purpose when its condition is not met. Treating that like any other error hides a lost race.
try_statement.each { t ->
    skip file.isTest
    def body = t.field('body')
    only !body.descendants('call').isEmpty() && body.descendants('call').any { k -> k.field('function').text ==~ /(\w+\.)*(put_item|update_item|delete_item)/ && k.keyword('ConditionExpression') != null }
    // An except clause for ClientError that neither re-raises nor tells the conditional failure apart from the others.
    def swallow = t.children.any { e -> e.type == 'except_clause' && e.text ==~ /(?s)\s*except\s*\(?\s*(\w+\.)*(ClientError|Exception).*/ && e.descendants('raise_statement').isEmpty() && !(e.text ==~ /(?s).*ConditionalCheckFailed.*/) }

    candidate t, "This conditional write fails when its condition is not met, and the except clause handles every ClientError alike without re-raising, so a lost race looks like success or like any other error. Catch ConditionalCheckFailedException on its own",
        swallow
}
