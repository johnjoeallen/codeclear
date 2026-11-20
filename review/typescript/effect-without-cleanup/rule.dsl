// generated-from: review/typescript/effect-without-cleanup/rule.md
// rule-hash: 65a8aad1e3bf4e88ef7d017cfa20306b67b09c7258408417f209d37f2737e419
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'effect-without-cleanup'

call_expression.each { c ->
    only c.field('function').text in ['useEffect', 'useLayoutEffect', 'React.useEffect', 'React.useLayoutEffect']
    skip file.isTest
    def cb = c.arg(0)
    only cb != null && cb.type in ['arrow_function', 'function_expression'] && cb.field('body').type == 'statement_block'
    // What the effect returns: the cleanup, its text, or a name that the effect hands back (the subscription's own unsubscribe).
    def ret = cb.field('body').child('return_statement')
    def cleanup = ret == null ? '' : ret.text
    def handsBack = ret != null && ret.children.any { x -> x.type in ['identifier', 'member_expression', 'call_expression'] }

    // Each registration needs its matching release in what the effect returns. "Do not flag": a one-shot setTimeout that only sets
    // state, an object created inside the effect (it goes away with the effect), and a handle that the effect returns.
    report c, "This effect starts a timer, listener, subscription or connection and returns no cleanup for it, so every run adds another: return a function that clears it",
        cb.descendants('call_expression').any { r -> r.field('function').text in ['setInterval', 'window.setInterval'] } && !(cleanup ==~ /(?s).*clearInterval.*/)
    report c, "This effect starts a timer and returns no cleanup for it: return a function that calls clearTimeout",
        cb.descendants('call_expression').any { r -> r.field('function').text in ['setTimeout', 'window.setTimeout']
            && !r.arg(0).descendants('call_expression').every { k -> k.field('function').text ==~ /set[A-Z]\w*/ } } && !(cleanup ==~ /(?s).*clearTimeout.*/)
    report c, "This effect adds an event listener and returns no cleanup for it, so every run adds another: return a function that calls removeEventListener",
        cb.descendants('call_expression').any { r -> r.field('function').type == 'member_expression' && r.field('function').field('property').text == 'addEventListener'
            && !r.field('function').field('object').declaredAsNewIn(cb) }
            && !(cleanup ==~ /(?s).*removeEventListener.*/)
    report c, "This effect subscribes and returns no unsubscribe, so every run adds another subscription: return the unsubscribe",
        cb.descendants('call_expression').any { r -> r.field('function').type == 'member_expression' && r.field('function').field('property').text == 'subscribe' }
            && !handsBack && !(cleanup ==~ /(?s).*(unsubscribe|unsub|dispose|off\().*/)
    report c, "This effect opens a connection and returns no cleanup that closes it: return a function that calls close()",
        cb.descendants('new_expression').any { r -> r.field('constructor').text in ['WebSocket', 'EventSource'] } && !(cleanup ==~ /(?s).*close.*/)
}
