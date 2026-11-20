// generated-from: review/python/mutable-default-argument/rule.md
// rule-hash: ccdda3d4703e7abe62e6c4b71605ac641f0178901be43f59c71c70475ef5f68c
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'mutable-default-argument'

def mutableLiteral = { v -> v.type in ['list', 'dictionary', 'set', 'list_comprehension', 'dictionary_comprehension', 'set_comprehension'] }
def mutableCall = { v -> v.type == 'call' && v.field('function').text ==~ /(\w+\.)*(list|dict|set|defaultdict|deque|bytearray|OrderedDict|Counter)/ }

// The default is made once, when the function is defined, and shared by every call.
Node.each { p ->
    only p.type in ['default_parameter', 'typed_default_parameter']
    skip file.isTest
    // "Do not flag": None, tuples, frozenset, strings and numbers are not in the list of mutable values.
    def v = p.field('value')
    only mutableLiteral(v) || mutableCall(v)

    report p, "The default of ${p.field('name').text} is a mutable value that is shared by every call: use None and create it inside the function"
}
