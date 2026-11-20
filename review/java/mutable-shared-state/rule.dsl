// generated-from: review/java/mutable-shared-state/rule.md
// rule-hash: 24de8e3759d4c4484c85e3216db9a8e05402d89898e17fbe6b885f8b10f2bb84
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'mutable-shared-state'

def singleton = { a -> a in ['Component', 'Service', 'Repository', 'Controller', 'RestController'] }
// "Do not flag": scopes other than singleton.
def scoped = { a -> a in ['Scope', 'RequestScope', 'SessionScope', 'ApplicationScope'] }
def mutableType = { t -> t in ['List', 'ArrayList', 'LinkedList', 'Map', 'HashMap', 'LinkedHashMap', 'TreeMap', 'Set', 'HashSet', 'TreeSet', 'Collection', 'Deque', 'ArrayDeque', 'Queue', 'StringBuilder', 'Date', 'SimpleDateFormat'] }
// "Do not flag": thread-safe holders.
def threadSafe = { t -> t ==~ /(Atomic|Concurrent|CopyOnWrite).*/ || t in ['ThreadLocal', 'LongAdder', 'ReentrantLock'] }

// Whether two threads really reach the state depends on scope and call paths, so this is a candidate for the AI.
FieldDecl.each { f ->
    skip file.isTest
    // "Do not flag": volatile single-writer state, and thread-safe types.
    skip f.isVolatile || threadSafe(f.typeName) || threadSafe(f.initializerTypeName)

    // A static field is shared by every thread: a non-final one that is assigned, or a mutable one that is modified.
    candidate f, "The static field ${f.name} is shared by every thread and is reassigned after class initialisation",
        f.isStatic && !f.isFinal && f.assignedAfterInit
    candidate f, "The static ${f.typeName} ${f.name} is shared by every thread and is modified after class initialisation",
        f.isStatic && f.isFinal && mutableType(f.typeName) && f.mutatedAfterInit
    candidate f, "The static ${f.typeName} ${f.name} is shared by every thread and is modified after class initialisation",
        f.isStatic && !f.isFinal && mutableType(f.typeName) && f.mutatedAfterInit

    // Singleton Spring beans: one instance serves every request.
    candidate f, "${f.name} is state of a singleton bean and is assigned in a method, so concurrent requests overwrite each other",
        !f.isStatic && !f.isFinal && f.classAnnotations.any(singleton) && !f.classAnnotations.any(scoped) && f.assignedAfterInit
}
