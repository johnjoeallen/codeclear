// generated-from: review/java/requires-new-pressure/rule.md
// rule-hash: 998b8d4707ffef4984a699b19d8fdd11e9bab706a8479cc5c9cd99ca8a4694cd
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'requires-new-pressure'

def listener = { a -> a in ['KafkaListener', 'RabbitListener', 'JmsListener', 'SqsListener', 'EventListener', 'Scheduled', 'StreamListener'] }

// How many times the loop runs, and whether the pool is at risk, is not in the code, so this is a candidate for the AI.
Call.each { c ->
    skip file.isTest
    // The callee starts a new transaction of its own, which takes a second connection while the caller keeps its own.
    only c.targetAnnotations().any { a -> a ==~ /(?s).*Transactional.*REQUIRES_NEW.*/ }
    // In a loop or a lambda (a stream), or in a batch or listener method: the calls multiply.
    def m = c.enclosingMethod
    only !c.enclosingIterations.isEmpty() || c.inside(Lambda) || c.inside(Loop) || (m != null && m.annotations.any(listener))
    // "Do not flag": a failure or audit record that must survive the rollback of the caller.
    skip c.name ==~ /(?i).*(audit|failure|error|dead|dlq|log|record).*/

    candidate c, "${c.name}() opens a new transaction (REQUIRES_NEW) for each iteration while the caller's transaction and connection stay open: a large batch can exhaust the connection pool"
}
