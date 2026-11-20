// generated-from: review/typescript/timer-listener-leak/rule.md
// rule-hash: d1d3f13642bde1224e8b9764fdbbad97704533f777a23e34af5fa6f83e5f3cf9
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'timer-listener-leak'

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function')
    def name = fn.type == 'member_expression' ? fn.field('property').text : fn.text
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition'])
    def stored = c.parent != null && c.parent.type in ['variable_declarator', 'assignment_expression']
    def clearedInFinally = f != null && f.descendants('finally_clause').any { x -> x.mentions('clearTimeout') || x.mentions('clearInterval') }
    def cleared = f != null && (f.mentions('clearTimeout') || f.mentions('clearInterval'))

    // An interval whose id is thrown away can never be cleared: it runs until the process ends.
    candidate c, "setInterval() is started and its id is not kept, so it can never be cleared: it keeps running (and keeps the process alive) for ever, and every call of this function starts another",
        fn.text in ['setInterval', 'window.setInterval'] && c.parent != null && c.parent.type == 'expression_statement' && f != null
    // A timer that is cleared only on the path that works: when the work throws, it fires later.
    candidate c, "The timer is cleared only after the work succeeds, not in a finally block: when the work throws, the timer is left running and fires later against state that is gone",
        fn.text in ['setTimeout', 'setInterval'] && stored && cleared && !clearedInFinally && f != null && !f.descendants('await_expression').isEmpty()
    // "Do not flag": a module-level singleton, registered once, and a once listener (it is removed by firing).
    candidate c, "${name}() adds a listener inside a function that can run many times, and nothing removes it: every call adds another, so memory grows and each event is handled many times. Remove it with off() when done, or register it once",
        name in ['on', 'addListener', 'addEventListener'] && f != null && !(f.text ==~ /(?s).*(\.off\(|removeListener|removeEventListener|\.once\(|signal\s*:|AbortSignal).*/)
            && !(f.field('name') != null && f.field('name').text ==~ /(?i).*(init|setup|start|bootstrap|connect|register|install|mount|listen|constructor).*/)
            && !(f.parent != null && f.parent.type == 'call_expression' && f.parent.field('function').text in ['useEffect', 'useLayoutEffect'])
}
