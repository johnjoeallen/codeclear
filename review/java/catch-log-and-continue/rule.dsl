// generated-from: review/java/catch-log-and-continue/rule.md
// rule-hash: 81f80b6a95df8097779bf46a8f1452eab9869542a06e19558c80bebd73c1be85
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'catch-log-and-continue'

// A statement that only logs: a logger call at INFO or above, or printStackTrace().
def isLog = { s ->
    s instanceof ExprStmt && s.expression instanceof Call
        && ((s.expression.name in ['info', 'warn', 'error'] && s.expression.scope.text ==~ /(?i)(this\.)?(log|logger)/)
            || s.expression.name == 'printStackTrace')
}
def isCleanup = { x -> x.name in ['close', 'delete', 'deleteIfExists', 'shutdown', 'disconnect', 'flush'] }
def isBoundary = { a -> a in ['ExceptionHandler', 'KafkaListener', 'RabbitListener', 'JmsListener', 'SqsListener', 'EventListener', 'Scheduled'] }

// Whether the caller needs to know about the failure depends on the call site, so this is a candidate for the AI.
Catch.each { c ->
    only !c.body.statements.isEmpty() && c.body.statements.every(isLog)
    skip file.isTest

    // "Do not flag": top-level boundaries that map the failure elsewhere (main, run, listeners, exception handlers) ...
    def m = c.enclosingMethod
    skip m != null && (m.name in ['main', 'run', 'call'] || m.annotations.any(isBoundary))
    // ... and best-effort cleanup where failure is irrelevant: the try block only closes or deletes.
    skip !c.tryCalls.isEmpty() && c.tryCalls.every(isCleanup)

    candidate c, "The catch block only logs the failure and carries on: does the caller need to know?"
}
