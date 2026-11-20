// generated-from: review/python/test-defects/rule.md
// rule-hash: 6a84ade9e449247a4d2c875d494e5def4f0330644ff5ca887baec4984435ea47
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'test-defects'

def pytestAsync = { f -> f.parent != null && f.parent.type == 'decorated_definition' && f.parent.children.any { d -> d.type == 'decorator' && d.text ==~ /(?s).*(asyncio|anyio|trio).*/ } }

Node.each { n ->
    only n.type in ['function_definition', 'assert_statement', 'call']
    skip file.path ==~ /(?i).*(integration|e2e|functional).*/
    def module = n.ancestor('module')
    def fn = n.type == 'call' ? n.field('function').text : ''
    def name = n.type == 'function_definition' ? n.field('name').text : ''

    // "Do not flag": a project in asyncio_mode = auto (shown by a pytestmark or setting) marks every async test.
    candidate n, "${name}() is an async test with no asyncio or anyio marker: without one pytest creates the coroutine and never runs it, so the test passes without testing anything",
        n.type == 'function_definition' && 'async' in n.tokens && name ==~ /test_\w*/ && !pytestAsync(n) && !(module.mentions('pytestmark') || module.mentions('asyncio_mode'))
    candidate n, "assert (condition, 'message') asserts a non-empty tuple, which is always true: the test can never fail. Write assert condition, 'message'",
        n.type == 'assert_statement' && n.childAt(0) != null && n.childAt(0).type == 'tuple'
    candidate n, "assert True can never fail: this line checks nothing",
        n.type == 'assert_statement' && n.childAt(0) != null && n.childAt(0).type == 'true'
    candidate n, "assertTrue(a, b) treats b as the failure message, so it only checks that a is truthy: use assertEqual(a, b) to compare",
        n.type == 'call' && fn ==~ /(self\.)?assertTrue/ && n.positional.size().isEqualTo(2) && n.arg(1).type != 'string'
    // "Do not flag": frozen time or a seeded random, which make the result repeatable.
    candidate n, "${fn}() makes this test depend on the clock, on chance or on the network, so it can fail on a slow or offline machine: freeze the time, seed the random, or mock the call",
        n.type == 'call' && (fn == 'time.sleep' || fn ==~ /datetime\.(datetime\.)?(now|utcnow|today)/ || (fn ==~ /random\.\w+/ && fn != 'random.seed') || fn ==~ /requests\.(get|post|put|patch|delete|head)/ || fn ==~ /socket\.(socket|create_connection)/)
            && !(module.mentions('freeze_time') || module.mentions('freezegun') || module.mentions('seed') || module.mentions('responses') || module.mentions('mock') || module.mentions('patch') || module.mentions('monkeypatch'))
}
