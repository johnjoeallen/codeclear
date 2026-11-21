// generated-from: review/config/removed-setting-still-read/rule.md
// rule-hash: 9127aa95e10e3425f71c0a4b1827d034fd864d500f60217d0316ed8dff7460cc
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'removed-setting-still-read'

// A key the change took out of this file, which a source file still reads (the rule needs the diff base to see it).
Removed.each { r ->
    skip file.isTest
    candidate r, "${r.rawKey} (was ${r.oldValue}) was removed from this file but is still read: ${project.references(r.key)}",
        !project.references(r.key).isEmpty() && !project.hasSetting(r.key)
}
