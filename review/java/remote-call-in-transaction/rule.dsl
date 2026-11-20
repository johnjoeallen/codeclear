// generated-from: review/java/remote-call-in-transaction/rule.md
// rule-hash: a740b8026eff171ef150ba8618f3cfbb186e1bc895879e5ff0fcf05825dd0c60
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'remote-call-in-transaction'

// Remote clients are recognised by the declared type of the receiver, so a local variable named client is not one.
def remote = { t -> t ==~ /.*(RestTemplate|RestClient|WebClient|HttpClient|Feign.*|.*FeignClient|KafkaTemplate|RabbitTemplate|AmqpTemplate|JmsTemplate|Sqs.*Client|Sns.*Client|SqsTemplate|SnsTemplate)(<.*>)?/ }

// How long the call holds the transaction depends on the client and on what was written before it, so this is a candidate for the AI.
Call.each { c ->
    only c.inTransaction
    skip file.isTest

    // "Do not flag": a handler that runs after the commit, and a method that suspends or forbids the transaction.
    def m = c.enclosingMethod
    skip m != null && m.annotations.any { a -> a in ['TransactionalEventListener'] }
    skip c.enclosingClassAnnotations.any { a -> a == 'TransactionalEventListener' }
    skip m != null && m.text ==~ /(?s).*@Transactional\s*\([^)]*propagation\s*=\s*(Propagation\.)?(NOT_SUPPORTED|NEVER).*/

    candidate c, "Thread.sleep() holds the database connection and its locks while the transaction is open",
        c.name == 'sleep' && c.scope.text in ['Thread', 'TimeUnit.SECONDS', 'TimeUnit.MILLISECONDS']
    candidate c, "${c.name}() is a remote call inside an open transaction: it holds a pooled connection and locks, and a failure after a write leaves side effects behind",
        c.scope != null && remote(c.scope.declaredType) && c.name != 'toString'
}
