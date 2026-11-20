// generated-from: review/typescript/test-unreliable-assertions/rule.md
// rule-hash: 2af47e433c7e8e62a6d494a0e200beb28528dc6119235346a94b7433887580bc
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'test-unreliable-assertions'

def testFn = { t -> t in ['it', 'test', 'specify', 'fit', 'xit'] }
def assertion = { s -> s ==~ /(?s).*(expect\(|assert[\.\(]|\.should\b|verify\(|toHaveBeen|\.to\.|check\().*/ }

call_expression.each { c ->
    def fn = c.field('function').text
    def module = c.ancestor('program')
    // "Do not flag": fake timers, seeded randomness and mocked network, which make time and chance repeatable.
    def faked = module.text ==~ /(?s).*(useFakeTimers|jest\.mock|nock\(|msw|sinon|setSystemTime|seedrandom|faker\.seed).*/

    candidate c, "${fn}.only is left in: the rest of the tests in this file are silently skipped, so the suite passes without running them. Remove .only",
        fn ==~ /(it|test|describe)\.only/ || fn in ['fit', 'fdescribe']
    // A test with no assertion cannot fail except by throwing.
    candidate c, "This test has no expect or assert, so it passes whatever the code does, as long as it does not throw",
        testFn(fn) && c.arg(1) != null && c.arg(1).type in ['arrow_function', 'function_expression'] && !assertion(c.arg(1).text)
    candidate c, "expect(true).toBe(true) can never fail: this test checks nothing",
        fn == 'expect' && c.arg(0) != null && c.arg(0).type == 'true' && c.parent != null && c.parent.type == 'member_expression' && c.parent.parent != null && c.parent.parent.text ==~ /(?s).*toBe\(\s*true\s*\).*/
    candidate c, "This test's only assertion is toBeDefined or toBeTruthy, which passes for almost any value: assert the value itself",
        testFn(fn) && c.arg(1) != null && c.arg(1).type in ['arrow_function', 'function_expression'] && c.arg(1).text ==~ /(?s)(?!.*expect\(.*expect\().*expect\([^;]*\)\.(toBeDefined|toBeTruthy|not\.toBeNull|not\.toBeUndefined)\(\).*/
    candidate c, "${fn}() makes this test depend on the clock, on chance or on the network, so it can fail on a slow or offline machine: use fake timers, a seeded random, or a mock",
        !faked && (fn in ['Date.now', 'Math.random'] || (fn in ['setTimeout'] && c.ancestorOf(['arrow_function', 'function_expression']) != null) || (fn == 'fetch' && c.arg(0) != null && c.arg(0).text ==~ /["'`]https?:\/\/(?!localhost|127\.0\.0\.1).*/))
}
