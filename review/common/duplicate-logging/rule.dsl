// generated-from: review/common/duplicate-logging/rule.md
// rule-hash: c8f6b201ab6067b543e81b522ab535d87350aa54f6e632a8d831b99a28e72ff8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'duplicate-logging'

def isBoundary = { a -> a in ['ExceptionHandler', 'KafkaListener', 'RabbitListener', 'JmsListener', 'SqsListener', 'EventListener'] }
def isAdvice = { a -> a in ['ControllerAdvice', 'RestControllerAdvice'] }

// Whether a caller logs the same failure again is not visible in this file, so this is a candidate for the AI.
Call.each { x ->
    only x.name in ['warn', 'error'] && x.scope.text ==~ /(?i)(this\.)?(log|logger)/
    skip file.isTest

    // A WARN or ERROR log call inside a catch block that passes the caught exception or its message ...
    def c = x.enclosingCatch
    only c != null && x.args.any { a -> a.mentions(c.param.name) }
    // ... and a throw in the same block: the exception or a wrapper that has it as its cause.
    only c.body.throws.any { t -> t.expression.carries(c.param.name) }

    // "Do not flag": the designated single logging point (listeners, exception handlers, controller advice).
    def m = x.enclosingMethod
    skip m != null && m.annotations.any(isBoundary)
    skip x.enclosingClassAnnotations.any(isAdvice)

    candidate x, "The exception is logged here and thrown again, so it is probably logged once per layer: log it where it is handled"
}
