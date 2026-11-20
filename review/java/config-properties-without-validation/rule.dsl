// generated-from: review/java/config-properties-without-validation/rule.md
// rule-hash: 849f9cbb5debf9ebfd6c3c5e9ad1c49410270de7aa2a9a03869ead993270623c
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'config-properties-without-validation'

def risky = { n -> n ==~ /(?i).*(timeout|url|uri|host|port|size|password|secret|token|interval|ttl|limit|max|min|count|threads).*/ }
def validated = { a -> a == 'Validated' }

// Whether an unset value fails at runtime depends on the profiles and on how the value is used, so this is a candidate for the AI.
FieldDecl.each { f ->
    only risky(f.name)
    skip file.isTest
    skip f.isStatic || f.isFinal

    // "Do not flag": a validated properties class, a field with a constraint, and a property with a default value.
    def bound = 'ConfigurationProperties' in f.classAnnotations && !f.classAnnotations.any(validated)
    skip f.text ==~ /(?s).*@(NotNull|NotBlank|NotEmpty|Min|Max|Positive|PositiveOrZero|Range|Size|Pattern|DurationMin|DurationMax)\b.*/
    skip f.hasInitializer && bound

    candidate f, "${f.name} is bound from configuration but the properties class is not @Validated: an unset or invalid value only fails at runtime",
        bound
    candidate f, "${f.name} is injected with a default of 0, null or an empty string: an unset property is not caught at startup",
        f.text ==~ /(?s).*@Value\(\s*"\$\{[^}:]+:(0|null|false)?\}"\s*\).*/
}
