// generated-from: review/common/lost-error-detail/rule.md
// rule-hash: 75e255e845561557030b6e55ed1d566da01c43392c0c2f28d19ebe6d52530902
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'lost-error-detail'

// Specific, expected failures where the message says enough.
def expected = { t -> t ==~ /(.*[|\s.])?(\w*(NotFound|Validation|Invalid|Parse|NumberFormat|Timeout|Conflict|BadRequest)\w*)([|\s].*)?/ }

Call.each { x ->
    only x.name in ['warn', 'error', 'fatal'] && x.scope.text ==~ /(?i)(this\.)?(log|logger)/
    skip file.isTest
    def c = x.enclosingCatch
    only c != null
    // "Do not flag": an expected condition, where the message is enough ...
    skip expected(c.param.type)
    // ... and a block that rethrows the original exception, which keeps the detail.
    skip c.body.throws.any { t -> t.expression.carries(c.param.name) }
    // The message or string form of the exception is logged ...
    only x.args.any { a -> a.calls.any { m -> m.name in ['getMessage', 'getLocalizedMessage', 'toString'] && m.scope.text == c.param.name } }
    // "Do not flag": the exception object passed as the cause is kept with its stack trace.
    skip x.args.any { a -> a.carries(c.param.name) }

    candidate x, "Only the message of '${c.param.name}' is logged, so the stack trace and the cause are lost: pass the exception itself as the last argument"
}
