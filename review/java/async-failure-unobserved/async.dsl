// generated-from: review/java/async-failure-unobserved/rule.md
// rule-hash: 097e1baf4c4f1ac2bee98923afe56ba7637132521a0b7cdbac78cde8590c1ab4
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'async-failure-unobserved'

// An @Async method that returns void: its exception goes to the executor's uncaught handler, which logs nothing by default.
Method.each { m ->
    only 'Async' in m.annotations && m.returnType == 'void'
    skip file.isTest
    // "Do not flag": a try/catch around the work, and an AsyncUncaughtExceptionHandler somewhere in the project.
    skip m.text ==~ /(?s).*\bcatch\b.*/
    skip project.mentions('AsyncUncaughtExceptionHandler')

    candidate m.nameNode, "${m.name}() is @Async and returns void, so a failure is not seen by the caller or logged: catch and record it, or return a CompletableFuture, or configure an AsyncUncaughtExceptionHandler"
}
