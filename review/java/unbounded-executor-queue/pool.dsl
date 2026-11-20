// generated-from: review/java/unbounded-executor-queue/rule.md
// rule-hash: 765fbfb1060b7e08066e13dbc7ffe7c755038e34f50b163fc1f88e971fd9b00e
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'unbounded-executor-queue'

New.each { n ->
    skip file.isTest

    // A ThreadPoolExecutor given an unbounded queue and no rejection handler. A bounded queue, or an explicit handler, is fine.
    candidate n, "This ThreadPoolExecutor has an unbounded queue and no rejection policy",
        n.typeName == 'ThreadPoolExecutor'
            && n.args.any { a -> a instanceof New && a.typeName in ['LinkedBlockingQueue', 'LinkedBlockingDeque', 'LinkedTransferQueue', 'PriorityBlockingQueue'] && a.args.isEmpty() }
            && !n.args.any { a -> a instanceof New && a.typeName ==~ /.*(Policy|Handler)/ }

    // A ThreadPoolTaskExecutor whose queue capacity is never set keeps the default, Integer.MAX_VALUE.
    candidate n, "This ThreadPoolTaskExecutor never sets a queue capacity, so its queue is effectively unbounded",
        n.typeName == 'ThreadPoolTaskExecutor' && (n.enclosingMethod == null || !n.enclosingMethod.calls.any { x -> x.name == 'setQueueCapacity' })
}
