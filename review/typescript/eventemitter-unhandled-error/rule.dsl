// generated-from: review/typescript/eventemitter-unhandled-error/rule.md
// rule-hash: 4811339bc9ff732eb00a685ad4cb6bd7cc1f14351cc81b2ffab23daec5c043eb
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'eventemitter-unhandled-error'

def emitterValue = { v -> v != null && ((v.type == 'new_expression' && v.field('constructor').text ==~ /(\w+\.)*(EventEmitter|Socket|WebSocket|Server|Readable|Writable|Transform|Duplex|PassThrough|Client)/)
    || (v.type == 'call_expression' && v.field('function').text ==~ /(net|tls|http|https|http2)\.(connect|createConnection|request|get|createServer)|(fs\.)?(createReadStream|createWriteStream)|child_process\.spawn|spawn/)) }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function')
    only fn.type == 'member_expression' && fn.field('property').text in ['on', 'addListener'] && fn.field('object').type == 'identifier'
    def event = c.arg(0)
    only event != null && event.text ==~ /["'](data|message|close|end|connect|open|readable|finish|drain|response|exit)["']/
    def recv = fn.field('object').text
    def module = c.ancestor('program')
    // The receiver is something that emits 'error': an emitter, a stream, a socket or a client created in this file.
    only module.descendants('variable_declarator').any { d -> d.field('name') != null && d.field('name').text == recv && emitterValue(d.field('value')) }
    // "Do not flag": an error listener on it, or pipeline, finished or events.once managing it.
    skip module.hasCallOn(recv, 'on', "'error'") || module.hasCallOn(recv, 'on', "\"error\"") || module.hasCallOn(recv, 'once', "'error'") || module.hasCallOn(recv, 'once', "\"error\"") || module.hasCallOn(recv, 'addListener', "'error'")
    skip module.text ==~ /(?s).*(pipeline\(|finished\(|events\.once|once\(\s*\w+\s*,\s*["']error).*/

    candidate c, "'${recv}' can emit 'error', and nothing listens for it: an unhandled 'error' event throws and takes the whole process down. Add ${recv}.on('error', ...), or use stream.pipeline"
}
