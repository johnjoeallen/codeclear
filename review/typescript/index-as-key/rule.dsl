// generated-from: review/typescript/index-as-key/rule.md
// rule-hash: 91cd102e6187ddd352311647748dacaa6e16d59b69bf5c8efd4e70fd06b31e9e
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'index-as-key'

jsx_attribute.each { a ->
    // A key attribute: <li key={...}>.
    only a.child('property_identifier') != null && a.child('property_identifier').text == 'key'
    skip file.isTest
    def value = a.child('jsx_expression')
    only value != null

    // The callback of the enclosing map(...): its second parameter is the index.
    def f = a.ancestorOf(['arrow_function', 'function_expression'])
    only f != null && f.parent != null && f.parent.type == 'arguments' && f.parent.parent.field('function').type == 'member_expression'
        && f.parent.parent.field('function').field('property').text == 'map'
    def p = f.field('parameters').childAt(1)
    only p != null
    def index = p.type == 'identifier' ? p : p.field('pattern')
    only index != null && value.mentions(index.text)

    // "Do not flag": a list that never reorders: an array literal, or a const array defined at the top of the file.
    def source = f.parent.parent.field('function').field('object')
    skip source.type == 'array'
    skip source.type == 'identifier' && a.ancestor('program').descendants('variable_declarator').any { d -> d.field('name').text == source.text
        && d.field('value') != null && d.field('value').type == 'array' && 'const' in d.parent.tokens && d.parent.parent.type == 'program' }

    report a, "The array index is used as the key, so when the list is reordered, filtered or inserted into, React reuses the wrong elements and their state: use a stable id from the data"
}
