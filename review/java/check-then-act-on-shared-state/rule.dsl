// generated-from: review/java/check-then-act-on-shared-state/rule.md
// rule-hash: e735cad6709f9da4c3fb27f887d0ed7de95c45d3bb36dfb89402290cbefe6b09
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'check-then-act-on-shared-state'

// A thread can run between the check and the act, so two threads both pass the check.
If.each { i ->
    skip file.isTest
    // "Do not flag": a local or thread-confined variable (not a field), and code inside synchronized.
    only i.guardedField != null
    skip i.inSynchronized

    candidate i, "The check and the put are separate steps on a shared map, so two threads can both pass the check and overwrite each other: use putIfAbsent or computeIfAbsent",
        i.text ==~ /(?s)\s*if\s*\(\s*!\s*(\w+)\.containsKey\([^)]*\)\s*\)\s*\{?\s*\1\.put\(.*/
            || i.text ==~ /(?s)\s*if\s*\(\s*(\w+)\.get\([^)]*\)\s*==\s*null\s*\)\s*\{?\s*\1\.put\(.*/
    candidate i, "This lazy initialisation of a shared field is not synchronized, so two threads can both see null and create two objects: use synchronized, a holder class or volatile with double-checked locking",
        i.text ==~ /(?s)\s*if\s*\(\s*(?:this\.)?(\w+)\s*==\s*null\s*\)\s*\{?\s*(?:this\.)?\1\s*=.*/ && !i.guardedField.isVolatile
    candidate i, "The read and the update of this atomic are separate steps, so two threads can both pass the check: use compareAndSet or updateAndGet",
        i.text ==~ /(?s)\s*if\s*\(\s*(\w+)\.get\(\)\s*[<>]=?[^)]*\)\s*\{?\s*\1\.(incrementAndGet|getAndIncrement|addAndGet|getAndAdd|set|decrementAndGet|getAndDecrement)\(.*/
}
