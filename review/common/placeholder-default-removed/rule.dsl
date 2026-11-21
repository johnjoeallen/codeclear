// generated-from: review/common/placeholder-default-removed/rule.md
// rule-hash: 6442744e3da4d78141d3a06481b23d35ae5cf7b72ba194fecb86a1a69032c6e6
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'placeholder-default-removed'

// A configuration value with a ${key} that had ${key:default} at the diff base, and no file of the run defines.
Setting.each { s ->
    only !project.droppedDefaults(file.path, s.value).isEmpty()
    candidate s, "${s.rawKey} no longer has a default for ${project.droppedDefaults(file.path, s.value)}: the placeholder used to be ${'$'}{key:default}, so an environment that does not define the key now fails at start-up",
        project.droppedDefaults(file.path, s.value).any { k -> !project.hasSetting(k) }
}
