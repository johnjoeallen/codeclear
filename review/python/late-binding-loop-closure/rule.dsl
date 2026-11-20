// generated-from: review/python/late-binding-loop-closure/rule.md
// rule-hash: 8115b232104ebeb971321610178a5279da52eee7040cc9f98b63b36692ed6b02
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'late-binding-loop-closure'

// Calls that keep a function and run it later, after the loop has finished and the variable has its last value.
def deferred = { n -> n in ['on_commit', 'add_done_callback', 'call_soon', 'call_later', 'call_at', 'submit', 'create_task', 'append', 'add', 'register', 'run_in_executor', 'schedule', 'Timer', 'Thread', 'setdefault'] }

Node.each { f ->
    only f.type in ['lambda', 'function_definition']
    skip file.isTest
    // The nearest loop, without leaving the function that the closure sits in.
    def loop = f.ancestorOf(['for_statement', 'while_statement', 'function_definition', 'lambda', 'class_definition'])
    only loop != null && loop.type in ['for_statement', 'while_statement']
    // It refers to the loop variable (or a name assigned in the loop) and does not bind it with a default (lambda x=x).
    def names = loop.type == 'for_statement' ? loop.field('left').descendants('identifier') : loop.descendants('assignment')
    def left = loop.field('left')
    def uses = loop.type == 'for_statement' && ((left.type == 'identifier' && f.field('body').mentions(left.text) && !f.bindsByDefault(left.text))
        || names.any { t -> f.field('body').mentions(t.text) && !f.bindsByDefault(t.text) })
    only uses
    // Handed to a call that keeps it, or stored. "Do not flag": functools.partial, and a callback run in the same iteration.
    def call = f.parent
    def kept = (call.type == 'argument_list' && deferred(call.parent.field('function').field('attribute').text))
        || (call.type == 'argument_list' && deferred(call.parent.field('function').text))
        || (call.type == 'keyword_argument' && call.field('name').text == 'target')
        || call.type in ['assignment', 'pair', 'list', 'tuple']
        || (f.type == 'function_definition' && loop.descendants('call').any { k -> k.mentions(f.field('name').text) && (deferred(k.field('function').text) || deferred(k.field('function').field('attribute').text)) })
    only kept

    candidate f, "This function uses the loop variable, but it runs after the loop is over, when the variable holds its last value: bind it with a default (lambda x=x: ...) or functools.partial"
}
