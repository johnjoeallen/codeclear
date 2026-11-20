// generated-from: review/java/async-failure-unobserved/rule.md
// rule-hash: 097e1baf4c4f1ac2bee98923afe56ba7637132521a0b7cdbac78cde8590c1ab4
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'async-failure-unobserved'

// Callers never see a failure of a task that nobody waits for, so it only ever shows up as a silent absence.
Call.each { c ->
    only c.isDiscarded
    skip file.isTest
    // "Do not flag": a task whose body catches and handles, and a chain that has an error handler.
    skip c.text ==~ /(?s).*\bcatch\b.*/
    skip c.text ==~ /(?s).*\.(exceptionally|handle|whenComplete|join|get)\s*\(.*/

    candidate c, "${c.name}() starts work and the returned future is thrown away, so a failure inside the task is never observed: keep the future and handle its failure, or catch and log inside the task",
        (c.name in ['submit', 'execute'] && c.scope.declaredType ==~ /(\w*\.)*\w*(Executor|ExecutorService|ScheduledExecutorService|ThreadPoolExecutor|TaskExecutor)(<.*>)?/)
            || c.name in ['runAsync', 'supplyAsync']
    candidate c, "This CompletableFuture chain has no exceptionally, handle or whenComplete and nobody joins it, so a failure is lost",
        c.name in ['thenApply', 'thenAccept', 'thenRun', 'thenCompose', 'thenApplyAsync', 'thenAcceptAsync', 'thenRunAsync']
            && c.text ==~ /(?s).*(supplyAsync|runAsync|CompletableFuture).*/
}
