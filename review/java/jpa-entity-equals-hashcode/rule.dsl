// generated-from: review/java/jpa-entity-equals-hashcode/rule.md
// rule-hash: fbf8b3e5e5848d0acf3df521304b3ba17ec819bd4e8c6e931e170495de88d88c
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'jpa-entity-equals-hashcode'

// An @Entity with Lombok's @Data or @EqualsAndHashCode gets equals and hashCode over every field, the generated id and the relationships included.
Annotation.each { a ->
    only a.name in ['Data', 'EqualsAndHashCode'] && 'Entity' in a.enclosingClassAnnotations
    // "Do not flag": equality on an explicitly chosen business key.
    skip a.text ==~ /(?s).*onlyExplicitlyIncluded\s*=\s*true.*/ || file.isTest

    candidate a, "@${a.name} on an entity builds equals and hashCode from every field, including the generated id and the relationships: they change while the entity sits in a hash collection"
}
