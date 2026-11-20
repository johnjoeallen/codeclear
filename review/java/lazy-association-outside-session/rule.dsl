// generated-from: review/java/lazy-association-outside-session/rule.md
// rule-hash: a5e2ee3664def61876276a1567b999e5168f63921427bc90bfcea8704e199f6b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'lazy-association-outside-session'

Call.each { c ->
    only c.lazyAssociation != null && c.dereferenced
    skip file.isTest

    def osivOff = project.config('spring.jpa.open-in-view') == 'false'
    // The entity was loaded by a join-fetch query or entity graph, possibly several calls away.
    skip c.scope.fetched

    report c.nameNode,
        "Lazy association '${c.lazyAssociation}' is read outside any transaction and open-in-view is disabled",
        !c.inTransaction && osivOff
    report c.nameNode,
        "Lazy association '${c.lazyAssociation}' is read in a loop: one query per iteration (N+1)",
        c.inside(Loop)
}
