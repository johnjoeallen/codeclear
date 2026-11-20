// generated-from: review/typescript/http-client-no-timeout/rule.md
// rule-hash: ac1e9dd313e9573aef7c4026557d127059157df6cd541d45e9d6d63f35a073d5
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'http-client-no-timeout'

// An options object that sets timeout or signal; a spread may hold one, so it counts too.
def bounded = { o -> o.children.any { p -> (p.type == 'pair' && p.field('key').text ==~ /["']?(timeout|signal)["']?/)
    || (p.type == 'shorthand_property_identifier' && p.text in ['timeout', 'signal']) || p.type == 'spread_element' } }
// The options argument is missing, or an object literal without a timeout. A variable may hold one, so it is not flagged.
def unbounded = { o -> o == null || (o.type == 'object' && !bounded(o)) }

call_expression.each { c ->
    skip file.isTest
    // "Do not flag": browser-only code.
    skip file.path ==~ /(?i).*(\.tsx|\.jsx|(^|\/)(public|client|frontend|browser)\/.*)/
    def fn = c.field('function').text

    report c, "fetch() has no signal, so it can wait forever on a stalled server: pass { signal: AbortSignal.timeout(ms) }",
        fn in ['fetch', 'globalThis.fetch'] && unbounded(c.arg(1))
    // axios.get/delete/head take the config second, post/put/patch third; axios.request takes it first.
    report c, "${fn}() has no timeout, so it can wait forever on a stalled server: pass { timeout: ms }",
        fn ==~ /axios\.(get|delete|head|options)/ && unbounded(c.arg(1))
    report c, "${fn}() has no timeout, so it can wait forever on a stalled server: pass { timeout: ms }",
        fn ==~ /axios\.(post|put|patch)/ && unbounded(c.arg(2))
    report c, "axios.request() has no timeout, so it can wait forever on a stalled server: pass { timeout: ms }",
        fn == 'axios.request' && unbounded(c.arg(0)) && c.arg(0) != null
    // "Do not flag": a client created once with a default timeout; a call on such an instance is not matched at all.
    report c, "axios.create() sets no timeout, so every call on this client can wait forever: pass { timeout: ms }",
        fn == 'axios.create' && unbounded(c.arg(0))
    report c, "${fn}() has no timeout, so it can wait forever on a stalled server: pass { timeout: ms }",
        fn ==~ /got(\.(get|post|put|patch|delete|head))?/ && unbounded(c.arg(1))
    report c, "${fn}() has no timeout, so it can wait forever on a stalled server: pass { timeout: ms }",
        fn ==~ /https?\.(request|get)/ && unbounded(c.arg(0)) && !(c.arg(1) != null && c.arg(1).type == 'object' && bounded(c.arg(1)))
}
