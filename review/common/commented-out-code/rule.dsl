// generated-from: review/common/commented-out-code/rule.md
// rule-hash: 446a134ea98db98316e324041b6199e349faf5fd4fff3f2e2a4bba05cf0dcc02
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'commented-out-code'

// A run of comment lines that read like code. Whether it is really disabled code is for the AI to judge.
Comment.each { c ->
    skip file.isTest
    // The first comment of a run is the one that has its size; a comment that continues a run has none.
    only c.blockSize.isGreaterThanOrEqualTo(3)
    // "Do not flag": a Javadoc comment with code examples, a license header, and a block that explains itself.
    skip c.text ==~ /(?s)\s*\/\*\*.*/
    skip c.blockText ==~ /(?is).*(copyright|license|licensed under|spdx-license).*/
    skip c.blockText ==~ /(?is).*(kept because|see |disabled until|todo|example|for instance|e\.g\.|note:).*/
    // At least 60 percent of the lines look like code.
    only c.codeScore.isGreaterThanOrEqualTo(60)

    candidate c, "This comment block looks like disabled code: delete it (version control keeps it), or say why it is kept"
}
