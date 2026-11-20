// generated-from: review/typescript/rabbitmq-publisher-confirms/rule.md
// rule-hash: e11fba8c301ba8bd9c1ce5378b4305615e6b8a9f8de90d9fa3fd1e3937a07493
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'rabbitmq-publisher-confirms'

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function')
    def name = fn.type == 'member_expression' ? fn.field('property').text : fn.text
    def module = c.ancestor('program')
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition'])
    // "Do not flag": best-effort telemetry and transient messages.
    skip module.text ==~ /(?is).*(telemetry|metrics?|analytics|best[- ]effort|transient).*/
    def options = name == 'publish' ? c.arg(3) : (name == 'sendToQueue' ? c.arg(2) : null)

    candidate c, "createChannel() gives a channel on which publish() returns before the broker has the message, so a message can be lost with no error: use createConfirmChannel() and wait for the confirm",
        name == 'createChannel' && module.text ==~ /(?s).*\.(publish|sendToQueue)\(.*/ && !(module.text ==~ /(?s).*(createConfirmChannel|waitForConfirms|confirm\s*:).*/)
    candidate c, "${name}() sends a message without { persistent: true }, so it is lost if the broker restarts, even though the queue is durable",
        name in ['publish', 'sendToQueue'] && module.text ==~ /(?s).*durable\s*:\s*true.*/ && (options == null || !(options.text ==~ /(?s).*persistent\s*:\s*true.*|.*deliveryMode\s*:\s*2.*/))
    // A connection or channel made in a function that runs per call, and not closed there. "Do not flag": a shared, long-lived one.
    candidate c, "${name}() opens a connection or channel inside a function that runs for each message or request, and the function never closes it: connections pile up until the broker refuses more",
        name in ['connect', 'createChannel', 'createConfirmChannel'] && f != null && !(f.text ==~ /(?s).*\.(close|disconnect)\(.*/) && !(f.field('name') != null && f.field('name').text ==~ /(?i).*(init|setup|start|connect|bootstrap|create).*/)
            && (fn.text ==~ /(amqp(lib)?\.connect|connect)|.*\.createChannel|.*\.createConfirmChannel/)
}
