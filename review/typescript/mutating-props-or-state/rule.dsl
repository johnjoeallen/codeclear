// generated-from: review/typescript/mutating-props-or-state/rule.md
// rule-hash: a11e414cf2e29f121fb1c51c7989b54f05f9940f8ad0a32061930eac43acad1a
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'mutating-props-or-state'

def mutator = { n -> n in ['push', 'pop', 'shift', 'unshift', 'splice', 'sort', 'reverse', 'fill', 'copyWithin'] }

Node.each { n ->
    only n.type in ['assignment_expression', 'call_expression', 'augmented_assignment_expression']
    skip file.isTest
    def module = n.ancestor('program')
    // "Do not flag": Immer drafts and Redux Toolkit reducers, which are written to mutate.
    skip module.text ==~ /(?s).*(immer|produce\(|createSlice|createReducer|useImmer).*/
    def f = n.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition'])
    only f != null
    // What is changed: the target of an assignment, or the array that a mutating method is called on.
    def callee = n.field('function')
    def target = n.type == 'call_expression' && callee.type == 'member_expression' && mutator(callee.field('property').text) ? callee.field('object') : n.field('left')
    only target != null && target.type in ['member_expression', 'subscript_expression', 'identifier']
    def root = target.rootName
    only root != ''
    def isAssign = n.type != 'call_expression'
    // A parameter that the function changes and then hands back or keeps: the caller's object is changed too.
    def param = f.field('parameters') != null && f.field('parameters').mentions(root)

    candidate n, "This changes props directly: props are read-only, so React does not see the change, and the parent's own object is changed behind its back. Copy it ({ ...props.x, y }) and pass the new value up",
        root == 'props' && (isAssign && target.type != 'identifier' || !isAssign)
    candidate n, "This changes a state variable in place: React compares references, so the component does not re-render, and the old render keeps the changed object. Build a new value and pass it to the setter",
        root != 'props' && n.stateDeclaredAround(root) && (!isAssign || target.type != 'identifier')
    candidate n, "This changes the parameter '${root}' in place and the function returns or keeps it, so the caller's own object changes too. Copy it first, or return a new value",
        root != 'props' && param && !n.stateDeclaredAround(root) && (!isAssign || target.type != 'identifier') && f.descendants('return_statement').any { r -> r.mentions(root) }
}
