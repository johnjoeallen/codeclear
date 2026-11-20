// generated-from: review/java/collection-modified-during-iteration/rule.md
// rule-hash: 215329ef59dfd47fd3a998dbbc076afacdc90501a82ce791d5ee74c3447bce12
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'collection-modified-during-iteration'

def mutators = ['add', 'remove', 'clear', 'put', 'addAll', 'removeAll']

Call.each { x ->
    only x.name in mutators && x.scope != null
    // The mutated collection is the one an enclosing for-each or forEach(...) is walking, or a view of it (keySet(),
    // values(), entrySet(), stream()). A copy such as new ArrayList<>(c) is a different expression, so it never matches.
    // "Do not flag": copy-on-write and concurrent collections, which allow it.
    only x.enclosingIterations.any { f -> f.iteratedCollection == x.scope.text && !(f.iteratedType ==~ /java\.util\.concurrent\..*/) }
    // "Do not flag": the mutation is the last thing before the loop is left with break, continue or return.
    skip x.followedByExit
    // "Do not flag": put(k, v) for the key being visited only replaces a value, which does not change the map's structure.
    skip x.updatesCurrentKey

    report x, "'${x.scope.text}' is changed while it is being iterated: this throws ConcurrentModificationException; use an iterator, removeIf or iterate a copy"
}
