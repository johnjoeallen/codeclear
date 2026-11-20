// generated-from: review/python/gather-taskgroup-error-semantics/rule.md
// rule-hash: 35c44df652f70096344e20f6c053b26cf9941c96941e013086c0433688c11294
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'gather-taskgroup-error-semantics'

def writes = { t -> t ==~ /(?is).*(save|write|send|post|put|delete|update|commit|insert|publish|charge|create|remove|transfer).*/ }
def functionTypes = ['function_definition', 'module']

call.each { c ->
    only c.field('function').text == 'asyncio.gather'
    skip file.isTest
    def scope = c.ancestorOf(functionTypes)
    def results = c.parent != null && c.parent.type == 'await' ? c.parent.parent : c.parent
    def rx = c.keyword('return_exceptions')

    // gather without return_exceptions cancels nothing when one task fails: the others keep running, and their side effects happen.
    candidate c, "When one of these tasks fails, gather() raises at once but the other tasks keep running, so their writes still happen and nothing cleans them up: use a TaskGroup, or catch the error and cancel the rest",
        c.args.size().isGreaterThanOrEqualTo(2) && rx == null && c.args.any { a -> writes(a.text) } && c.ancestors('try_statement').isEmpty()
    // return_exceptions=True turns every failure, cancellation included, into a returned value that has to be looked at.
    candidate c, "return_exceptions=True puts failures in the result list, and this code never checks the results for exceptions, so a failed task looks like it succeeded",
        rx != null && rx.type == 'true' && (results.type == 'expression_statement' || !(scope.mentions('isinstance') || scope.mentions('BaseException') || scope.mentions('Exception')))
}
