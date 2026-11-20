// generated-from: review/java/jpa-entity-equals-hashcode/rule.md
// rule-hash: fbf8b3e5e5848d0acf3df521304b3ba17ec819bd4e8c6e931e170495de88d88c
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'jpa-entity-equals-hashcode'

// A hashCode over a database-generated id is null before the entity is saved and different afterwards. A hashCode that
// returns getClass().hashCode() does not mention the id, so it is not matched; equals on the id is fine with that hashCode.
Method.each { m ->
    only m.name == 'hashCode' && 'Entity' in m.enclosingClassAnnotations
    skip file.isTest

    candidate m.nameNode, "hashCode() uses the generated id, which changes when the entity is saved: the entity is lost from any HashSet or HashMap it is already in",
        m.usesGeneratedId
}
