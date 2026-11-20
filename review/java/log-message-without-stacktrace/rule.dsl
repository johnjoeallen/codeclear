// generated-from: review/java/log-message-without-stacktrace/rule.md
// rule-hash: 0e6dc1a38197f82cebfbc5f77d6b5a1e072512c90d8d8ca045778801e4f03ae5
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'log-message-without-stacktrace'

Call.each { x ->
    only x.name in ['warn', 'error'] && x.scope.text ==~ /(?i)(this\.)?(log|logger)/
    skip file.isTest

    // Inside a catch (a call outside any catch has no catch, so this test fails) of a broad or unchecked exception. Expected, specific exceptions (NumberFormatException,
    // ParseException, TimeoutException, ...) are not in this list, so they are not flagged.
    def c = x.enclosingCatch
    only c.param.type ==~ /(.*[|\s.])?(Exception|RuntimeException|Throwable|IllegalArgumentException|IllegalStateException|NullPointerException|ClassCastException|IndexOutOfBoundsException|UnsupportedOperationException|ArithmeticException)([|\s].*)?/

    // The message or string form of the exception is logged (e.getMessage(), e.getLocalizedMessage(), e.toString()) ...
    only x.args.any { a -> a.calls.any { m -> m.name in ['getMessage', 'getLocalizedMessage', 'toString'] && m.scope.text == c.param.name } }
    // ... and the exception itself is not passed on, so the stack trace is lost.
    skip x.args.any { a -> a.carries(c.param.name) }

    report x, "Only the message of '${c.param.name}' is logged: pass the exception itself as the last argument to keep the stack trace"
}
