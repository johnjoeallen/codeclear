// generated-from: review/python/sql-string-formatting/rule.md
// rule-hash: 846f286cbaf7f294f65008d86202ed2bb85359097b82197089b9979b6ec618e8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'sql-string-formatting'

// A string that starts like a statement: SELECT, INSERT, UPDATE or DELETE, after an optional prefix such as f or b.
def sqlStart = { a -> a.text ==~ /(?is)\s*[a-z]{0,2}("""|'''|"|')\s*(select|insert|update|delete)\b.*/ }

call.each { c ->
    skip file.isTest
    def fn = c.field('function')
    only (fn.type == 'attribute' && fn.field('attribute').text in ['execute', 'executemany', 'raw']) || fn.text in ['text', 'sqlalchemy.text']
    // The statement is the first argument; a second one holds the parameters, which is the safe form.
    def a = c.arg(0)
    only a != null && sqlStart(a)

    // "Do not flag": a constant string, and placeholders with separate parameters, are not built by formatting.
    report c, "The SQL is built with an f-string: pass the values as parameters (%s, ?, :name) so the driver quotes them",
        a.type == 'string' && !a.descendants('interpolation').isEmpty()
    report c, "The SQL is built with %: pass the values as the parameters argument so the driver quotes them",
        a.type == 'binary_operator' && '%' in a.tokens
    report c, "The SQL is built by concatenation with a value: pass it as a parameter so the driver quotes it",
        a.type == 'binary_operator' && '+' in a.tokens && !a.descendants('identifier').isEmpty()
    report c, "The SQL is built with .format(): pass the values as parameters so the driver quotes them",
        a.type == 'call' && a.field('function').type == 'attribute' && a.field('function').field('attribute').text == 'format'
}
