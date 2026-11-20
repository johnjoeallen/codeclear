// generated-from: review/python/shared-mutable-class-or-cached-state/rule.md
// rule-hash: a82882490cb87d9d2bac0c30d67f36a3d6b622fcf972551c79a5575e55c48a2d
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'shared-mutable-class-or-cached-state'

def mutableValue = { v -> v != null && (v.type in ['list', 'dictionary', 'set', 'list_comprehension', 'dictionary_comprehension', 'set_comprehension']
    || (v.type == 'call' && v.field('function').text in ['set', 'dict', 'list', 'defaultdict', 'deque', 'OrderedDict', 'collections.defaultdict', 'collections.deque'])) }

Node.each { n ->
    only n.type in ['assignment', 'function_definition']
    skip file.isTest
    // A class attribute: an assignment directly in a class body (not in a method).
    def owner = n.ancestorOf(['class_definition', 'function_definition', 'lambda'])
    def cls = n.ancestor('class_definition')
    def left = n.type == 'assignment' ? n.field('left') : null

    // "Do not flag": a class attribute that no method changes through self.
    candidate n, "${left.text} is a list, dict or set shared by every instance of the class, and a method changes it through self: instances see each other's data. Create it in __init__",
        n.type == 'assignment' && owner != null && owner.type == 'class_definition' && left.type == 'identifier' && mutableValue(n.field('right')) && cls.mutatesSelfAttribute(left.text)
    // A parameter kept on self and then changed: the caller's own object is changed too.
    candidate n, "${left.text} keeps the caller's object, and this class then changes it in place, so the caller's data changes too. Copy it first: ${left.text} = list(...)",
        n.type == 'assignment' && owner != null && owner.type == 'function_definition' && left.type == 'attribute' && left.field('object').text == 'self'
            && n.field('right').type == 'identifier' && n.field('right').usesParameterOf(owner) && cls != null && cls.mutatesSelfAttribute(left.field('attribute').text)
    // A cached function that hands out its own list, dict or set: the next caller gets what the last one changed.
    candidate n, "${n.field('name').text}() is cached and returns a mutable list, dict or set: every caller gets the same object, so one caller's change reaches all the others. Return a tuple, frozenset or a copy",
        n.type == 'function_definition' && n.parent != null && n.parent.type == 'decorated_definition'
            && n.parent.children.any { d -> d.type == 'decorator' && d.text ==~ /(?s).*(lru_cache|cache|cached_property).*/ }
            && n.descendants('return_statement').any { r -> r.children.any { x -> mutableValue(x) } }
}
