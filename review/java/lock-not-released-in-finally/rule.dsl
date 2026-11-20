// generated-from: review/java/lock-not-released-in-finally/rule.md
// rule-hash: 25684aa50cd18c5d10c25404e3dbf57aecfbbe9c27c97cee9abeb0b4e829117f
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'lock-not-released-in-finally'

Call.each { c ->
    only c.name in ['lock', 'lockInterruptibly', 'acquire']

    // A Lock or Semaphore by its resolved type; failing that, a receiver whose name says it is one (reported at LOW).
    def isLock = c.name == 'acquire' ? c.scope.type == 'java.util.concurrent.Semaphore'
                                     : c.scope.type ==~ /java\.util\.concurrent\.locks\..*Lock.*/
    def guessed = c.scope.type == null && c.scope.text ==~ /(?i).*(lock|semaphore).*/
    only isLock || guessed

    // The release belongs in the finally of a try that starts right after the acquire.
    // The common variant, try { lock.lock(); ... } finally { lock.unlock(); }, does release it, so it is not reported either.
    skip c.releasedInNextFinally(['unlock', 'release']) || c.releasedInEnclosingFinally(['unlock', 'release'])

    // Acquire and release split across two methods of one class is not certain to be wrong.
    report c, "The lock is not released in a finally block that follows the acquire; an exception leaves it held",
        confidence: guessed || c.releasedElsewhere(['unlock', 'release']) ? LOW : DEFAULT
}
