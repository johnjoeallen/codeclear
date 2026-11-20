// generated-from: review/java/completablefuture-blocking-or-unbounded-wait/rule.md
// rule-hash: 555847c6fc804f0f4a6959048424fe18edb5a31a6060102e523432c8a76e356f
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'completablefuture-blocking-or-unbounded-wait'

def blocking = { k -> (k.name in ['sleep', 'join', 'take', 'block', 'exchange', 'getForObject', 'postForObject', 'retrieve', 'queryForObject', 'readAllBytes', 'readAllLines', 'executeQuery'])
    || (k.name == 'get' && k.args.isEmpty()) }
def future = { t -> t ==~ /(CompletableFuture|Future|CompletionStage|ForkJoinTask)(<.*>)?/ }

// Whether the stage can hang or starve a thread depends on the execution context, so this is a candidate for the AI.
Call.each { x ->
    skip file.isTest
    // "Do not flag": a bounded wait, and a chain that sets a timeout.
    skip x.scope.text ==~ /(?s).*\b(orTimeout|completeOnTimeout)\b.*/

    candidate x, "${x.name}() waits for a future with no timeout: if the stage never completes the thread waits forever",
        x.name in ['get', 'join'] && x.args.isEmpty()
            && (future(x.scope.declaredType) || x.scope.text ==~ /(?s).*CompletableFuture\s*\.\s*(supplyAsync|runAsync|allOf|anyOf)\(.*/)
    // The default pool is shared by the whole JVM: blocking work in it starves other stages.
    candidate x, "${x.name}() runs blocking work on the shared default pool: pass an executor",
        x.name in ['supplyAsync', 'runAsync', 'thenApplyAsync', 'thenAcceptAsync', 'thenRunAsync'] && x.args.size().isEqualTo(1)
            && x.firstArg.calls.any(blocking)
    candidate x, "thenCompose() blocks on another future inside the stage: compose it instead",
        x.name == 'thenCompose' && x.firstArg.calls.any { k -> k.name == 'join' }
}
