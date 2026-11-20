// generated-from: review/typescript/async-callback-in-foreach/rule.md
// rule-hash: 067f308f383b6000371ce1fb41eb4e1c0bf59aec80bd8f4af366bbb68f6c50e9
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'async-callback-in-foreach'

call_expression.each { c ->
    def fn = c.field('function')
    only fn.type == 'member_expression' && fn.field('property').text == 'forEach'
    // An async callback: forEach does not wait for the promises it returns.
    def cb = c.arg(0)
    only cb != null && cb.type in ['arrow_function', 'function_expression', 'function'] && 'async' in cb.tokens

    report c, "forEach() does not wait for an async callback: its errors are lost and the loop ends before the work does. Use for...of with await, or Promise.all(items.map(...))"
}
