// generated-from: review/python/bare-except/rule.md
// rule-hash: 39a6dad6d81f2a95890436355d598eb26712b1496f80391a98b0bdf7199129c0
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'bare-except'

// A handler with no type, or one that catches BaseException, and does not end by re-raising.
except_clause.each { c ->
    skip file.isTest
    def types = c.children.every { x -> x.type == 'block' }
    only types || c.children.any { x -> x.type != 'block' && x.text ==~ /(builtins\.)?BaseException(\s+as\s+\w+)?/ }
    // "Do not flag": a handler that re-raises after cleanup.
    def last = c.child('block').lastChild
    skip last != null && last.type == 'raise_statement' && last.children.isEmpty()

    report c, "This except clause catches everything, including KeyboardInterrupt and SystemExit: name the exceptions that are expected, or re-raise"
}
