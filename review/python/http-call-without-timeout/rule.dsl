// generated-from: review/python/http-call-without-timeout/rule.md
// rule-hash: 2b7352ea3410cbd03e9d417fb2dfede590d5ccd51af9b63ab6d2381c7c9772b8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'http-call-without-timeout'

def verbs = ['get', 'post', 'put', 'patch', 'delete', 'head', 'options', 'request']

// No timeout means a call can wait forever. timeout=None means the same.
call.each { c ->
    skip file.isTest
    def fn = c.field('function')
    def timeout = c.keyword('timeout')
    // The call may pass its options as **kwargs, which can hold the timeout.
    skip c.args.any { a -> a.type == 'dictionary_splat' }
    def missing = timeout == null || timeout.type == 'none'

    report c, "${fn.text}() has no timeout, so it can wait forever on a stalled server: pass timeout=(connect, read)",
        missing && fn.text ==~ /requests\.(get|post|put|patch|delete|head|options|request)/
    report c, "${fn.text}() has no timeout, so it can wait forever on a stalled server: pass timeout=",
        missing && fn.text ==~ /(urllib\.request\.)?urlopen/
    // "Do not flag": a Session whose timeout is set by an adapter in the same file. httpx has a default, so it is not matched.
    report c, "${fn.text}() on a requests Session has no timeout: pass timeout=, or mount an adapter that sets one",
        missing && fn.type == 'attribute' && fn.field('attribute').text in verbs && fn.field('object').text ==~ /(?i)(\w+\.)*\w*session\w*/
            && c.ancestor('module').mentions('requests') && !c.ancestor('module').mentions('HTTPAdapter')
}
