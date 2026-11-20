// generated-from: review/python/validate-after-normalization/rule.md
// rule-hash: d3bf20d8a524a7a99c940264ecf0e646685dd3e697d74f5dbb612d9a16a36352
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'validate-after-normalization'

// The value that was checked is not the value that is used: decoding or normalising after the check can undo it.
function_definition.each { f ->
    skip file.isTest
    // "Do not flag": a containment check on the final path.
    skip f.mentions('is_relative_to') || f.mentions('commonpath')
    def a = f.normalizedAfterCheck()
    only a != null

    candidate a, "${a.field('left').text} is checked above and then changed here (decoded, normalised or lower-cased): the check was made on a different value than the one used, so '%2e%2e/' or '..\\' can slip through. Normalise first, then check, and use the checked value"
}
