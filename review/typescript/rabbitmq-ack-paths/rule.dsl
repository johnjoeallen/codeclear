// generated-from: review/typescript/rabbitmq-ack-paths/rule.md
// rule-hash: a01dd4530baf07b8e620d97a3d9b7e9032d41f1eea1e48f591f1467c6282734f
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'rabbitmq-ack-paths'

def settle = { k -> k.field('function').type == 'member_expression' && k.field('function').field('property').text in ['ack', 'nack', 'reject', 'ackAll', 'nackAll'] }

call_expression.each { c ->
    only c.field('function').type == 'member_expression' && c.field('function').field('property').text == 'consume'
    skip file.isTest
    def cb = c.arg(1)
    only cb != null && cb.type in ['arrow_function', 'function_expression']
    // "Do not flag": noAck: true, where losing a message is accepted.
    skip c.args.any { a -> a.type == 'object' && a.text ==~ /(?s).*noAck\s*:\s*true.*/ }
    def acks = cb.descendants('call_expression').any(settle)
    // A try whose catch or finally settles the delivery on every path.
    def settledOnError = cb.descendants('try_statement').any { t -> t.descendants('call_expression').any(settle) && (t.child('catch_clause') != null || t.child('finally_clause') != null) }

    candidate c, "This consumer never acknowledges a message (no ack, nack or reject, and no noAck): the broker keeps every delivery unacknowledged until the channel closes, then redelivers them all",
        !acks
    candidate c, "This consumer awaits work with no try/catch that settles the delivery: if the work throws, the message is never acked or nacked, and it stays unacknowledged until the channel closes",
        acks && !cb.descendants('await_expression').isEmpty() && !settledOnError
    candidate c, "The message is acknowledged before the awaited work is done: if the work then fails, the message is already gone and is lost",
        acks && cb.text ==~ /(?s).*\.ack\(.*\bawait\b.*/ && !(cb.text ==~ /(?s).*\bawait\b.*\.ack\(.*/)
}
