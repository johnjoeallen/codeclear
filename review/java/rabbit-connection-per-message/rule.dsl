// generated-from: review/java/rabbit-connection-per-message/rule.md
// rule-hash: b2fbef221359ad01b2318c3822350686e43d8c2da1bc0976a0f7503bc25cff5d
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'rabbit-connection-per-message'

def factory = { t -> t ==~ /(\w*\.)*(ConnectionFactory|CachingConnectionFactory)(<.*>)?/ }
def connection = { t -> t ==~ /(\w*\.)*Connection(<.*>)?/ }

// Opening a connection or a channel for each message is slow, and leaks one when it is not closed.
Call.each { c ->
    only (c.name in ['newConnection', 'createConnection'] && factory(c.scope.declaredType)) || (c.name == 'createChannel' && connection(c.scope.declaredType))
    skip file.isTest
    // "Do not flag": a channel made inside execute(ChannelCallback), and a connection made once in lifecycle or init code.
    skip 'execute' in c.enclosingCalls
    def m = c.enclosingMethod
    skip m == null || m.annotations.any { a -> a in ['PostConstruct', 'Bean'] } || m.name in ['init', 'start', 'afterPropertiesSet', 'setup', 'connect', 'initialize']
    // "Do not flag": a try-with-resources used once for a bounded batch. In a loop it is one per message, so it is still reported.
    skip c.inTryResource && !c.inside(Loop)
    // A method that publishes or consumes one message, or a loop.
    only c.inside(Loop) || m.text ==~ /(?s).*(basicPublish|basicGet|basicConsume|convertAndSend|basicAck).*/

    candidate c, "${c.name}() opens a connection or channel for each message: create it once, share it, or use a Spring CachingConnectionFactory with a RabbitTemplate"
}
