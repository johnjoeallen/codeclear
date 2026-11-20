// generated-from: review/typescript/producer-send-unobserved/rule.md
// rule-hash: cf00d77b8b207a6fd20f4f899633ff90fb44790a3747c4c6217937ff72bfce0a
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'producer-send-unobserved'

// A send that nobody awaits: the promise is thrown away, so a message that cannot be sent is lost without a trace.
expression_statement.each { s ->
    skip file.isTest
    def c = s.child('call_expression')
    only c != null
    def fn = c.field('function')
    only fn.type == 'member_expression'
    def name = fn.field('property').text
    def recv = fn.field('object').text
    def scope = s.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    // "Do not flag": explicit best-effort telemetry.
    skip scope.text ==~ /(?is).*(telemetry|metrics?|analytics|best[- ]effort|fire[- ]and[- ]forget).*/

    candidate s, "${recv}.${name}() is not awaited, so if the message cannot be sent the error is an unhandled rejection or is lost, and the code carries on as if it had been delivered. Await it inside try/catch, or handle the result",
        (name in ['send', 'produce', 'sendBatch', 'publish', 'sendToQueue'] && recv ==~ /(?i)(\w+\.)*\w*(producer|sqs|sns|kafka|publisher|channel|eventbridge|kinesis|pubsub|queue)\w*/)
    // Shutting down right after a send that was not awaited loses the message that is still in the buffer.
    candidate s, "process.exit() right after a send that was not awaited: the message is still in the buffer and is lost. Await the send and flush the producer first",
        fn.text == 'process.exit' && s.previous != null && s.previous.type == 'expression_statement' && s.previous.child('call_expression') != null
            && s.previous.child('call_expression').field('function').text ==~ /(?i).*(producer|sqs|sns|kafka|publisher|channel)\w*\.(send|publish|produce|sendToQueue)/
}
