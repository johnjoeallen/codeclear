// generated-from: review/java/shared-unsafe-collection/rule.md
// rule-hash: 69586c901181b14f8ae2d48ec53857c4114217f4dd83125d3b103634f7deb0ae
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'shared-unsafe-collection'

def singleton = { a -> a in ['Component', 'Service', 'Repository', 'Controller', 'RestController', 'Configuration'] }
def unsafe = { t -> t in ['HashMap', 'LinkedHashMap', 'TreeMap', 'HashSet', 'ArrayList', 'LinkedList', 'ArrayDeque'] }

// Only field declarations are visited. A collection typed as the interface still counts through its initializer, and one
// wrapped by Collections.synchronizedX or built as a ConcurrentHashMap has neither the type nor the initializer.
FieldDecl.each { f ->
    only unsafe(f.typeName) || unsafe(f.initializerTypeName)
    only f.isStatic || f.classAnnotations.any(singleton)
    skip file.isTest

    // Filled in a constructor or @PostConstruct only, or every change is inside synchronized code: nothing to report.
    candidate f, "The ${f.typeName} ${f.name} is shared by every request and is changed outside a constructor, but it is not thread-safe",
        f.mutatedAfterInit
}
