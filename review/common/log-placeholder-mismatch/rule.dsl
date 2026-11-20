// generated-from: review/common/log-placeholder-mismatch/rule.md
// rule-hash: 60278e4c73d66cb61ee0cbd11369e95a74dc0ed774381dce7ac2d413db69220a
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'log-placeholder-mismatch'

Call.each { c ->
    only c.name in ['trace', 'debug', 'info', 'warn', 'error'] && c.scope.text ==~ /(?i)(this\.)?(log|logger)/
    // "Do not flag": a format that is not a string literal (nothing to count), and tests. String.format and printf are
    // not logger calls, so the receiver test above leaves them out. The order of the arguments is not checked.
    only c.firstArg instanceof StringLit
    // Numbered placeholders, {0}, belong to other logging styles (java.util.logging, MessageFormat, Tinylog): not counted here.
    skip file.isTest || c.firstArg.text ==~ /(?s).*\{\d.*/

    // logArguments leaves out a trailing exception that has no placeholder: SLF4J treats it as the throwable.
    report c, "The format has ${c.firstArg.placeholders} placeholder(s) but the call passes ${c.logArguments} argument(s)",
        c.firstArg.placeholders != c.logArguments
}
