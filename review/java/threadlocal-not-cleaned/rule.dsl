// generated-from: review/java/threadlocal-not-cleaned/rule.md
// rule-hash: 4f6b5b26186430c22006e1200d0fddedfa620642b8e7e319e948cd2de14f1f9c
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'threadlocal-not-cleaned'

// Per-thread state set on a pooled thread stays there for the next task unless it is removed in a finally block.
Call.each { c ->
    skip file.isTest

    def isThreadLocal = c.name == 'set' && (c.scope.type == 'java.lang.ThreadLocal' || c.scope.declaredType ==~ /(Inheritable)?ThreadLocal.*/)
    def isMdc = c.scope.text == 'MDC' && c.name == 'put'
    def isSecurity = c.scope.text == 'SecurityContextHolder' && c.name == 'setContext'
    def isLocale = c.scope.text == 'LocaleContextHolder' && c.name in ['setLocale', 'setLocaleContext']
    def isRequest = c.scope.text == 'RequestContextHolder' && c.name == 'setRequestAttributes'
    only isThreadLocal || isMdc || isSecurity || isLocale || isRequest

    // set(null) is itself a way to clear the value.
    skip c.firstArg.text == 'null'

    // The value is removed in a finally block of the same method, or by a callback of the same class (afterCompletion, doFinally).
    skip c.releasedInMethodFinally(['remove', 'clear', 'clearContext', 'resetLocaleContext', 'resetRequestAttributes'])
    skip c.releasedElsewhere(['remove', 'clear', 'clearContext', 'resetLocaleContext', 'resetRequestAttributes'])

    // Putting a previous value back: the set and the release are the two branches of one if.
    skip c.pairedRelease(['remove', 'clear', 'clearContext', 'resetLocaleContext', 'resetRequestAttributes'])

    // A ThreadLocal.withInitial cache of small objects (a SimpleDateFormat) is not a leak of request state.
    report c, "${c.scope.text}.${c.name}() sets per-thread state that is not removed in a finally block: on a pooled thread it leaks into the next task",
        confidence: c.holderInitializer ==~ /(?s).*withInitial.*/ ? LOW : DEFAULT
}
