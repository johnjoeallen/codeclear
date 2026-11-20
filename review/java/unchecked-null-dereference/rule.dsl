// generated-from: review/java/unchecked-null-dereference/rule.md
// rule-hash: c5b0ad1749f8df057978ba5f3f29560b353055848b9a613d8941f3d9fb775154
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unchecked-null-dereference'

// A call made directly on a value that can be null.
Call.each { c ->
    only c.scope instanceof Call
    skip file.isTest
    def inner = c.scope
    def m = c.enclosingMethod
    // "Do not flag": a check, a default, or a requireNonNull in the same method, and a map that the code just filled.
    skip m != null && m.text ==~ /(?s).*(containsKey|requireNonNull|getOrDefault|computeIfAbsent|!= null|== null|isPresent|\.put\().*/
    // "Do not flag": a return type that says it is never null.
    skip inner.targetAnnotations().any { a -> a ==~ /(?s).*(NonNull|NotNull|Nonnull).*/ }

    candidate c, "get() on a map returns null when the key is missing, and ${c.name}() is called on the result with no check: a missing key throws NullPointerException",
        inner.name == 'get' && inner.args.size().isEqualTo(1) && inner.scope.declaredType ==~ /(\w*\.)*(Map|HashMap|LinkedHashMap|TreeMap|ConcurrentHashMap)(<.*>)?/
    candidate c, "get() on a JsonNode returns null for a missing field, and ${c.name}() is called on the result with no check: use path(), or check for null",
        inner.name == 'get' && inner.scope.declaredType ==~ /(\w*\.)*JsonNode/
    // A repository method that is not declared to return Optional can return null.
    candidate c, "${inner.name}() can return null when nothing is found, and ${c.name}() is called on the result with no check: return Optional, or check for null",
        inner.name ==~ /(find|get|load|lookup)By\w+/ && inner.scope.declaredType ==~ /(\w*\.)*\w*(Repository|Dao|DAO|Mapper)(<.*>)?/
            && !inner.targetReturnTypes().isEmpty() && inner.targetReturnTypes().every { t -> !(t ==~ /(Optional|List|Set|Collection|Page|Stream|Iterable|Slice)(<.*>)?/) }
}
