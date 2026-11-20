// generated-from: review/typescript/missing-return-after-response/rule.md
// rule-hash: d08bf8a9c40a40736f018f539e3a4950b04c16754da13e7215a942746ac7a7a9
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'missing-return-after-response'

def respond = { s -> s.text ==~ /(?s)\s*(await\s+)?(res|response|reply)\.(send|json|sendStatus|end|redirect|status|render|sendFile)\b.*/
    || s.text ==~ /(?s)\s*next\s*\(\s*\w.*/ }

if_statement.each { i ->
    skip file.isTest
    // An Express-style handler: the enclosing function takes (req, res).
    def f = i.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition'])
    only f != null && f.field('parameters') != null && f.field('parameters').text ==~ /(?s).*\b(res|response|reply)\b.*/
    // The last statement of the branch: a response or next(err) that is not followed by a return or a throw.
    def branch = i.field('consequence')
    def last = branch.type == 'statement_block' ? branch.lastChild : branch
    only last != null && last.type == 'expression_statement' && respond(last)
    // "Do not flag": the final statement of the handler. Something after the if that responds or does I/O runs after the response.
    only i.following.any { s -> s.mentions('res') || s.mentions('response') || s.mentions('reply') || !s.descendants('await_expression').isEmpty() }

    report last, "The response is sent but the handler does not return, so the code after the if still runs and may respond a second time (ERR_HTTP_HEADERS_SENT) or repeat the work: write 'return res.send(...)'"
}
