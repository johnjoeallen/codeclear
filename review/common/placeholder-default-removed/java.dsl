// generated-from: review/common/placeholder-default-removed/rule.md
// rule-hash: 6442744e3da4d78141d3a06481b23d35ae5cf7b72ba194fecb86a1a69032c6e6
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'placeholder-default-removed'

// A string in Java code with a ${key} that had ${key:default} at the diff base, and no file of the run defines.
StringLit.each { t ->
    skip file.isTest
    only !project.droppedDefaults(file.path, t.text).isEmpty()
    candidate t, "The placeholder for ${project.droppedDefaults(file.path, t.text)} no longer has a default: it used to be ${'$'}{key:default}, so an environment that does not define the key now fails at start-up",
        project.droppedDefaults(file.path, t.text).any { k -> !project.hasSetting(k) }
}
