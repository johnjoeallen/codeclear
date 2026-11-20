// generated-from: review/java/optional-field-or-parameter/rule.md
// rule-hash: 63fc818343deaacde8cfb5eada074f5db19425a247e1f513e6940ac1a793a829
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'optional-field-or-parameter'

// An Optional parameter forces every caller to wrap a value, and still allows null. Private methods are not checked.
Param.each { p ->
    only p.type ==~ /Optional<.*/ && p.inPublicMethod
    skip file.isTest

    candidate p, "The parameter ${p.name} of a public method is an Optional: use an overload or a nullable parameter"
}
