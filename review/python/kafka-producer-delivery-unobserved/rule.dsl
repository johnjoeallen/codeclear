// generated-from: review/python/kafka-producer-delivery-unobserved/rule.md
// rule-hash: e650fe59c7b9875d2326c712a339fa1227f766f70aaa07a88426f860df495ee7
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'kafka-producer-delivery-unobserved'

// send() only queues the message. Nobody learns that it failed unless something checks the future or flushes.
expression_statement.each { s ->
    skip file.isTest
    def c = s.child('call')
    only c != null
    def fn = c.field('function')
    only fn.type == 'attribute' && fn.field('attribute').text in ['send', 'produce'] && fn.field('object').text ==~ /(?i)(\w+\.)*\w*(producer|kafka)\w*/
    def scope = s.ancestorOf(['function_definition', 'module'])
    // "Do not flag": a delivery callback or errback, a flush or close on the same path, and send_and_wait (a different name).
    skip c.keyword('on_delivery') != null || c.keyword('callback') != null
    skip scope.mentions('flush') || scope.mentions('add_errback') || scope.mentions('add_callback') || scope.mentions('stop') || scope.mentions('close') || scope.mentions('poll')

    candidate s, "${fn.text}() only queues the message: its result is thrown away and nothing flushes, so a message that cannot be delivered is lost without a trace. Check the future, pass on_delivery, or flush before returning"
}
