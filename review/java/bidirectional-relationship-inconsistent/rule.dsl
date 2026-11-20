// generated-from: review/java/bidirectional-relationship-inconsistent/rule.md
// rule-hash: 00c77ed62530b7e9c95348d19cf2a90556d20a574e31429e29cb5bac065738b3
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'bidirectional-relationship-inconsistent'

// A one-sided change to a collection that the other entity mirrors with a back reference.
Method.each { m ->
    only m.enclosingClass != null && 'Entity' in m.enclosingClassAnnotations
    skip file.isTest
    // "Do not flag": a helper that updates both sides: it also sets the back reference (to this, or to null on removal).
    skip m.text ==~ /(?s).*\.set\w+\(\s*(this|null)\s*\).*/

    // A change to a collection field that has a mappedBy (the inverse side). A relationship without mappedBy is unidirectional.
    candidate m.nameNode, "${m.name}() changes a collection that is mapped by the other side, but does not set the back reference: the two sides disagree until the graph is reloaded, and the database is not updated",
        m.calls.any { k -> k.name in ['add', 'remove', 'addAll', 'removeAll', 'clear'] && k.scope instanceof Name && k.scope.fieldDeclText ==~ /(?s).*(OneToMany|ManyToMany)\s*\([^)]*mappedBy.*/ }
}
