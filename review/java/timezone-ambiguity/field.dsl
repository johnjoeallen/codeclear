// generated-from: review/java/timezone-ambiguity/rule.md
// rule-hash: 4c0406f21799d5bdc32f4f6ff6bdab84a85953d1108a33f5fd848dcf1c51686b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'timezone-ambiguity'

// A LocalDateTime or LocalDate carries no zone, so two services that exchange or store it can read it differently.
FieldDecl.each { f ->
    only f.typeName in ['LocalDateTime', 'LocalDate'] && !f.isStatic
    skip file.isTest

    candidate f, "The field ${f.name} is a ${f.typeName} in a class that is stored or exchanged: it has no time zone, so use an Instant or an OffsetDateTime",
        f.inDataClass
}
