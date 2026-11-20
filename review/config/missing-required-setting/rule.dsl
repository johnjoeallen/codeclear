// generated-from: review/config/missing-required-setting/rule.md
// rule-hash: 3a9fa504725025d093054acef3aa3f8109730eadec57ac7e5d73e667b7e5b2f9
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'missing-required-setting'

// A placeholder that names a key no file defines: nothing in the project can supply it.
Setting.each { s ->
    only !s.value.isEmpty()
    skip file.isTest
    // "Do not flag": an environment variable (the usual upper-case names), which the deployment provides, and a placeholder with a default.
    candidate s, "${s.rawKey} refers to ${'$'}{...}, a key that no configuration file of this run defines and that has no default: if the environment does not supply it either, the application fails at start-up",
        s.placeholders.any { k -> !(k ==~ /[A-Z][A-Z0-9_]*/) && !(k ==~ /(?i)(env|systemProperties|random|vcap)\..*/) && !project.hasSetting(k) }
}
