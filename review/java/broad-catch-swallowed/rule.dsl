// generated-from: review/java/broad-catch-swallowed/rule.md
// rule-hash: 4078e62c36cd08022d12ca5f6128c0bc7acc1e414c8af645a4a5eb6e66dc4bf9
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'broad-catch-swallowed'

def isBoundary = { a -> a in ['ExceptionHandler', 'KafkaListener', 'RabbitListener', 'JmsListener', 'SqsListener', 'EventListener', 'Scheduled'] }
def isLog = { s ->
    s instanceof ExprStmt && s.expression instanceof Call
        && ((s.expression.name in ['trace', 'debug', 'info', 'warn', 'error'] && s.expression.scope.text ==~ /(?i)(this\.)?(log|logger)/)
            || s.expression.name == 'printStackTrace')
}
// A return that looks like success: nothing, null, true, or an empty collection or Optional.
def okValue = { s ->
    s instanceof Return && (s.expression == null
        || s.expression.text in ['null', 'true', 'false', '0', 'Optional.empty()', 'List.of()', 'Set.of()', 'Map.of()',
                                 'Collections.emptyList()', 'Collections.emptySet()', 'Collections.emptyMap()', 'new ArrayList<>()'])
}
def stays = { s -> !(s instanceof Return) && !(s instanceof Throw) }

// Whether the site is a legitimate boundary depends on its callers, so this is a candidate for the AI.
Catch.each { c ->
    only c.param.type in ['Exception', 'Throwable', 'RuntimeException']
    skip file.isTest
    // An empty block and a block that only logs have rules of their own.
    only !c.body.statements.isEmpty() && !c.body.statements.every(isLog)

    // "Do not flag": a block that throws (rethrowing an Error subclass included), and one that records the failure somewhere.
    skip !c.body.throws.isEmpty() || !c.body.assignments.isEmpty()
    // "Do not flag": top-level boundaries that map the failure to a response, status or dead-letter.
    def m = c.enclosingMethod
    skip m != null && (m.name in ['main', 'run'] || m.annotations.any(isBoundary))

    candidate c, "Catching ${c.param.type} and carrying on makes the caller see success after a failure",
        c.body.statements.any(okValue) || c.body.statements.every(stays)
}
