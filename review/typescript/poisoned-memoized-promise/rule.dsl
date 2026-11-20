// generated-from: review/typescript/poisoned-memoized-promise/rule.md
// rule-hash: 6089e034ebb567de73fe42ff8fb7763324d1638e336789056fe720cb7213b581
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'poisoned-memoized-promise'

// A promise kept at module level and reused: when it rejects once, every later caller gets the same rejection.
Node.each { a ->
    only a.type in ['assignment_expression', 'augmented_assignment_expression']
    skip file.isTest
    def module = a.ancestor('program')
    def left = a.field('left')
    def right = a.field('right')
    only left != null && left.type == 'identifier' && right != null
    // The right side is a call that returns a promise (not awaited), and the variable is declared at the top of the module.
    only right.type == 'call_expression' && !(right.text ==~ /(?s).*\.catch\(.*/)
    only module.children.any { s -> s.type in ['lexical_declaration', 'variable_declaration'] && s.mentions(left.text) }
    // "Do not flag": a reset on failure, where the variable is set back to undefined or null.
    skip module.descendants('assignment_expression').any { r -> r.field('left').text == left.text && r.field('right').type in ['undefined', 'null'] }
    skip !a.ancestors('try_statement').isEmpty()

    candidate a, "${left.text} keeps the promise from ${right.field('function').text}() for every later caller, and nothing resets it when it rejects: one failed call (a network blip at start-up) fails every later call until the process restarts. Reset it in a .catch: ${left.text} = undefined; throw e"
}
