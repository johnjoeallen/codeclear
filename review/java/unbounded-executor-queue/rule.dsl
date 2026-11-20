// generated-from: review/java/unbounded-executor-queue/rule.md
// rule-hash: 765fbfb1060b7e08066e13dbc7ffe7c755038e34f50b163fc1f88e971fd9b00e
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'unbounded-executor-queue'

// Executors.newFixedThreadPool and newSingleThreadExecutor queue work without limit, and newCachedThreadPool starts threads
// without limit. Whether the pool serves request or message work is for the AI to judge.
Call.each { c ->
    only c.scope.text == 'Executors' && c.name in ['newFixedThreadPool', 'newSingleThreadExecutor', 'newCachedThreadPool']
    skip file.isTest

    candidate c, "Executors.${c.name}() has no bound on its queue or its threads: a burst of work exhausts memory or threads"
}
