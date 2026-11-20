// generated-from: review/typescript/test-unreliable-assertions/rule.md
// rule-hash: 2af47e433c7e8e62a6d494a0e200beb28528dc6119235346a94b7433887580bc
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'test-unreliable-assertions'

// expect(promise).resolves... as a statement: it returns a promise that nothing waits for.
expression_statement.each { s ->
    def c = s.child('call_expression')
    only c != null && c.text ==~ /(?s)\s*expect\(.*\)\.(resolves|rejects)\..*/
    candidate s, "This .resolves/.rejects assertion is not awaited or returned, so it runs after the test has finished and a failure is never reported. Write await expect(...).resolves..."
}
