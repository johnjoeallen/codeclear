// generated-from: review/java/log-and-rethrow/rule.md
// rule-hash: 905f46a3ebd03795ed833da5748d6489e901c41bd74c26e3f7c60e618771c6ee
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'log-and-rethrow'

def isBoundary = { a -> a in ['ExceptionHandler', 'KafkaListener', 'RabbitListener', 'JmsListener', 'SqsListener', 'EventListener'] }
def isAdvice = { a -> a in ['ControllerAdvice', 'RestControllerAdvice'] }

// Whether the failure is also logged upstream is not visible in this file, so this is a candidate for the AI.
Catch.each { c ->
    skip file.isTest

    // A WARN or ERROR log call that passes the caught exception or its message ...
    only c.body.calls.any { x -> x.name in ['warn', 'error'] && x.scope.text ==~ /(?i)(this\.)?(log|logger)/ && x.args.any { a -> a.mentions(c.param.name) } }
    // ... and a throw in the same block: the exception or a wrapper around it.
    only !c.body.throws.isEmpty()

    // "Do not flag": boundary methods (listeners, exception handlers, controller advice) that log and translate.
    def m = c.enclosingMethod
    skip m != null && m.annotations.any(isBoundary)
    skip c.enclosingClassAnnotations.any(isAdvice)

    candidate c, "The exception is logged and then thrown again, so it is probably logged twice: log it where it is handled, or here and not upstream"
}
