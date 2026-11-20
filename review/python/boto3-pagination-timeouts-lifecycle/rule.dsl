// generated-from: review/python/boto3-pagination-timeouts-lifecycle/rule.md
// rule-hash: 83467400ec54e77a304dfb1bdf53e17c787b4963af65c9a08dc6e1c269c1b985
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'boto3-pagination-timeouts-lifecycle'

def listCall = { n -> n in ['list_objects', 'list_objects_v2', 'scan', 'query'] || n ==~ /(list|describe)_\w+/ }

call.each { c ->
    skip file.isTest
    def module = c.ancestor('module')
    only module.mentions('boto3') || module.mentions('botocore') || module.mentions('aioboto3')
    def fn = c.field('function')
    def name = fn.type == 'attribute' ? fn.field('attribute').text : fn.text
    def scope = c.ancestorOf(['function_definition', 'module'])
    def f = c.ancestor('function_definition')
    def loop = c.ancestorOf(['for_statement', 'while_statement', 'function_definition', 'lambda'])

    // "Do not flag": a paginator, a continuation token loop, and a MaxKeys where the first page is what is wanted.
    candidate c, "${name}() returns one page (up to 1000 items) and the result is used as if it were everything: use get_paginator('${name}'), or loop on the continuation token",
        listCall(name) && name != 'query' && fn.type == 'attribute' && !(scope.mentions('NextToken') || scope.mentions('ContinuationToken') || scope.mentions('LastEvaluatedKey') || scope.mentions('ExclusiveStartKey') || scope.mentions('get_paginator') || scope.mentions('paginate') || scope.mentions('NextMarker') || scope.mentions('Marker') || scope.mentions('MaxKeys'))
    // A client made in a loop or in a request handler: each one builds its own connection pool and loads the service model again.
    candidate c, "${fn.text}() creates a new client each time it runs, so each request pays to build a connection pool and load the service model: create it once at module level and reuse it",
        fn.text in ['boto3.client', 'boto3.resource', 'boto3.Session', 'aioboto3.Session']
            && ((loop != null && loop.type in ['for_statement', 'while_statement']) || (f != null && !(f.field('name').text in ['__init__', 'main', 'get_client', 'init', 'setup', 'create_client', 'client'])))
    // "Do not flag": a Config with timeouts, and a client made at module level (Lambda init time).
    candidate c, "${fn.text}() has no Config with connect_timeout, read_timeout and retries, so a slow service blocks the request for the default 60 seconds, and a retry storm is not capped",
        fn.text in ['boto3.client', 'boto3.resource'] && c.keyword('config') == null && f != null
}
