// generated-from: review/python/resource-leak-on-exit/rule.md
// rule-hash: 663c30f2164810485337fc76396df4341c66cd0c2db3f16b5017038a42444010
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'resource-leak-on-exit'

def opens = { t -> t in ['open', 'socket.socket', 'subprocess.Popen', 'Session', 'requests.Session', 'urllib.request.urlopen', 'tempfile.NamedTemporaryFile', 'io.open'] }

Node.each { n ->
    only n.type in ['assignment', 'expression_statement']
    skip file.isTest
    def scope = n.ancestorOf(['function_definition', 'module'])
    def right = n.type == 'assignment' ? n.field('right') : n.child('call')
    only right != null && right.type == 'call'
    def left = n.field('left')
    def fn = right.field('function').text
    // "Do not flag": a with block, closing(), an ExitStack, and a handle that the function returns or keeps on self.
    skip !n.ancestors('with_statement').isEmpty() || scope.mentions('ExitStack') || scope.mentions('closing')
    // The release must be in a finally: a close() after the work is skipped when the work raises.
    def released = !scope.descendants('finally_clause').isEmpty() && scope.descendants('finally_clause').any { f -> f.mentions('close') || f.mentions('release') || f.mentions('terminate') || f.mentions('kill') || f.mentions('wait') }
    def handedOn = left != null && (left.type == 'attribute' || scope.descendants('return_statement').any { r -> r.mentions(left.text) })

    candidate n, "${fn}() is opened outside a with block and its close() is not in a finally: if the code in between raises or returns early, the handle stays open until the process ends. Use 'with'",
        left != null && left.type == 'identifier' && (opens(fn) || (fn == 'requests.get' && right.keyword('stream') != null && right.keyword('stream').type == 'true')) && !released && !handedOn
    candidate n, "acquire() with no release in a finally: if the work raises, the lock is never released and everyone else waits forever. Use 'with lock:'",
        n.type == 'expression_statement' && right.field('function').type == 'attribute' && right.field('function').field('attribute').text == 'acquire' && !released
}
