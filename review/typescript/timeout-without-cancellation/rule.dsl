// generated-from: review/typescript/timeout-without-cancellation/rule.md
// rule-hash: 7ec799b41ea1128b32c7a3985f07b9773475364003ac45ebdc64a596d641d171
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'timeout-without-cancellation'

def timerPromise = { a -> a != null && (a.text ==~ /(?s).*(setTimeout|delay\(|sleep\(|timeout\(|wait\().*/) && !(a.text ==~ /(?s).*(signal|AbortController|AbortSignal).*/) }

call_expression.each { c ->
    only c.field('function').text == 'Promise.race'
    skip file.isTest
    def module = c.ancestor('program')
    def list = c.arg(0)
    only list != null && list.type == 'array'
    // "Do not flag": an AbortSignal or signal option given to the work that is raced.
    skip list.text ==~ /(?s).*(signal|AbortSignal|AbortController).*/
    // One operand is a timer that rejects, the other is the work. Losing the race does not stop the work.
    candidate c, "Promise.race with a timer rejects when the time is up, but the raced work keeps running: it still holds its connection and makes its side effects after the caller was told it failed. Pass an AbortSignal (AbortSignal.timeout(ms)) into the work",
        list.children.any { a -> timerPromise(a) } && list.children.any { a -> !timerPromise(a) }
}
