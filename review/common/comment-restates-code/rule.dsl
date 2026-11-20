// generated-from: review/common/comment-restates-code/rule.md
// rule-hash: e61d82f8e09cbc310e5c749efd8305ced83b2b06047eb71e0901a09e9e1ff4f5
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'comment-restates-code'

// The comment says again what the code beside it says. One finding per file, and only when there are three or more.
Comment.each { c ->
    skip file.isTest
    // "Do not flag": Javadoc, license headers, tool directives and comments that give a reason are not line comments that restate: the fact skips them.
    only c.restatingFirst && c.restatingInFile.isGreaterThanOrEqualTo(3)

    candidate c, "Several comments in this file only repeat the code next to them (like this one): they add reading and go stale. Keep the comments that say why, and delete the ones that say what"
}
