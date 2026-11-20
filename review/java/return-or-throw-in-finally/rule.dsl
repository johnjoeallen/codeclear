// generated-from: review/java/return-or-throw-in-finally/rule.md
// rule-hash: 35bd23c8c671b1c71de30d56816dea8f3627934e4c778b0040e371269dfd8268
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'return-or-throw-in-finally'

// The host decides whether a statement leaves a finally block: not inside a nested lambda, class or method, not a
// break or continue whose loop is inside the finally, and not a throw caught by a nested try.
Jump.each { j ->
    only j.escapesFinally

    report j, "'${j.text}' in a finally block discards the try's result and any exception in flight"
}
