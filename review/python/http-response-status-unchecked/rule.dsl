// generated-from: review/python/http-response-status-unchecked/rule.md
// rule-hash: 3cad476fb9b4d5aac8030d814fa19304bdaf1e166d02d9a9ce8e00926af5ba45
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'http-response-status-unchecked'

def client = { t -> t ==~ /(requests|httpx)\.(get|post|put|patch|delete|head|request)/ || t ==~ /\w*(session|client)\w*\.(get|post|put|patch|delete|head|request)/ }
def functionTypes = ['function_definition', 'module']

// The response is used before anything says that the request succeeded.
assignment.each { a ->
    skip file.isTest
    def right = a.field('right')
    // The call, with the await in front of it if there is one.
    def call = right.type == 'await' ? right.child('call') : right
    only call != null && call.type == 'call' && client(call.field('function').text)
    def v = a.field('left')
    only v.type == 'identifier'
    def scope = a.ancestorOf(functionTypes)
    // "Do not flag": raise_for_status, a comparison of the status, and a client configured with raise_for_status in this file.
    skip scope.mentions('raise_for_status') || scope.mentions('status_code') || scope.mentions('status') || scope.mentions('ok')
    skip a.ancestor('module').mentions('raise_for_status')
    // The body is read.
    only scope.descendants('attribute').any { t -> t.field('object').text == v.text && t.field('attribute').text in ['json', 'text', 'content'] }

    candidate a, "${v.text} is used as if the request succeeded: a 404 or 500 response has a body too, so .json() parses an error page or raises a confusing error. Call ${v.text}.raise_for_status() first"
}
