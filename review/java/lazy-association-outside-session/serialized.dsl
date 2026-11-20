// generated-from: review/java/lazy-association-outside-session/rule.md
// rule-hash: a5e2ee3664def61876276a1567b999e5168f63921427bc90bfcea8704e199f6b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'lazy-entity-serialized'

Method.each { m ->
    only m.inController && m.returnedEntity != null && !m.lazyAssociations.isEmpty()
    skip file.isTest

    def osivOff = project.config('spring.jpa.open-in-view') == 'false'
    only osivOff
    skip m.returnsFetched

    report m.nameNode,
        "${m.name}() returns entity ${m.returnedEntity} whose lazy associations ${m.lazyAssociations} are serialised after the session has closed"
}
