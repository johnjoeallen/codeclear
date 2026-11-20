// generated-from: review/typescript/sql-injection-dynamic-query/rule.md
// rule-hash: ad25e5f4b1fcd21d0046d5992de9b06b6e74be70b98299ce373a1c56fbd19038
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'sql-injection-dynamic-query'

// A query string with values pasted into it, or built by concatenation.
def built = { a -> a != null && ((a.type == 'template_string' && !a.descendants('template_substitution').isEmpty())
    || (a.type == 'binary_expression' && '+' in a.tokens && !a.descendants('identifier').isEmpty())) }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function')
    def name = fn.type == 'member_expression' ? fn.field('property').text : fn.text
    // A tagged template parameterizes its values: sql`...` and prisma.$queryRaw`...` pass the template itself as the arguments.
    skip c.field('arguments').type == 'template_string'
    only name in ['query', 'execute', 'raw', '$queryRawUnsafe', '$executeRawUnsafe', 'queryRawUnsafe', 'run', 'all', 'get', 'prepare']
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    // Only when the string looks like SQL: the keyword is at the start of its literal part.
    only built(c.arg(0)) && c.arg(0).text ==~ /(?is)\s*[`"']\s*(select|insert|update|delete|with|create|alter|drop)\b.*/
    // "Do not flag": a function that checks the value against an allow-list of identifiers.
    skip f.text ==~ /(?s).*(allowlist|allowedColumns|ALLOWED|whitelist|\.includes\(|sanitizeIdentifier|escapeId|format\().*/

    candidate c, "${name}() is given SQL with values pasted into the string: a value such as \"' OR 1=1 --\" changes the query. Pass the values as parameters ($1, ?), and check a column or direction against an allow-list"
}
