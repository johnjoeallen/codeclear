// generated-from: review/java/optional-field-or-parameter/rule.md
// rule-hash: 63fc818343deaacde8cfb5eada074f5db19425a247e1f513e6940ac1a793a829
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'optional-field-or-parameter'

// An Optional field is not serializable and is not understood by JPA or by most JSON binders: in a class that is stored or
// exchanged it should be a nullable field, with an Optional getter if callers want one. Whether this project handles it is for the AI.
FieldDecl.each { f ->
    only f.typeName == 'Optional' && !f.isStatic
    skip file.isTest

    candidate f, "The field ${f.name} is an Optional in a class that is stored or exchanged: use a nullable field and return an Optional from the getter",
        f.inDataClass
}
