// generated-from: review/typescript/overlapping-async-interval/rule.md
// rule-hash: 46628c8d61971febd5a09e0701ddcf89ea8a713699fd3b4aa1581318ee4318bd
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'overlapping-async-interval'

call_expression.each { c ->
    only c.field('function').text in ['setInterval', 'window.setInterval', 'global.setInterval']
    skip file.isTest
    def cb = c.arg(0)
    only cb != null && cb.type in ['arrow_function', 'function_expression', 'function']
    // The callback waits for something: it is async, or awaits.
    only 'async' in cb.tokens || !cb.descendants('await_expression').isEmpty()
    // "Do not flag": a callback that checks a running or busy flag before it starts. (A callback with only synchronous work never gets here.)
    skip cb.descendants('if_statement').any { i -> i.field('condition').text ==~ /(?is).*(running|busy|inflight|in_flight|locked|pending|active|isrunning).*/ }

    report c, "The async callback of setInterval() is started again every period even if the last run has not finished, so runs overlap: use a running flag cleared in finally, or a setTimeout that schedules the next run when this one ends"
}
