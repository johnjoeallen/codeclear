// generated-from: review/typescript/queue-requeue-loop-dlq/rule.md
// rule-hash: db90e0e617acb68b14e234da97339259d2fafc8b46c634e59a99badfd9d282ba
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'queue-requeue-loop-dlq'

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function')
    def name = fn.type == 'member_expression' ? fn.field('property').text : fn.text
    def module = c.ancestor('program')
    def scope = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    // "Do not flag": a retry counter or a redelivery check in the callback, and a dead-letter exchange declared in the file.
    def bounded = scope.text ==~ /(?is).*(x-death|attempts?|retry|retries|redelivered|delay|maxRetries|headers\[).*/

    // Requeueing in an error branch with nothing to stop it: a message that always fails goes round forever.
    candidate c, "${name}() puts a failing message straight back on the queue with no retry count, so a message that always fails is redelivered forever and starves the others. Count attempts (x-death, a header) and dead-letter after a few",
        c.ancestor('catch_clause') != null && !bounded && ((name == 'nack' && c.arg(2) != null && c.arg(2).type == 'true') || (name == 'reject' && c.arg(1) != null && c.arg(1).type == 'true')
            || c.args.any { a -> a.type == 'object' && a.children.any { p -> p.type == 'pair' && p.field('key').text == 'requeue' && p.field('value').type == 'true' } })
    candidate c, "This queue has no deadLetterExchange, and the file rejects or nacks messages: a message that cannot be processed has nowhere to go but back on the queue, or the floor",
        name == 'assertQueue' && !(module.text ==~ /(?s).*(deadLetterExchange|x-dead-letter-exchange).*/) && module.text ==~ /(?s).*\.(nack|reject)\(.*/
            && !(c.arg(1) != null && c.arg(1).text ==~ /(?s).*(deadLetter|x-dead-letter).*/)
}
