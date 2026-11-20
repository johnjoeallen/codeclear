// generated-from: review/typescript/unchecked-network-data/rule.md
// rule-hash: 3ddaeb48fbc5761b87ea3fb146bcd8661714c98f35ec6a50308f0f98f8e806b8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unchecked-network-data'

// A type assertion tells the compiler what the data is; it checks nothing at run time.
Node.each { n ->
    only n.type in ['as_expression', 'non_null_expression']
    skip file.isTest
    def f = n.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    // "Do not flag": data passed through a schema validator before the assertion.
    skip f.text ==~ /(?s).*(zod|io-ts|yup|ajv|valibot|superstruct|safeParse|\.parse\(\s*(await|res|response|data|body)|validate\(|Joi|class-validator|typebox|Value\.Check).*/
    def inner = n.childAt(0)
    only inner != null
    def fromNetwork = inner.text ==~ /(?s).*(await\s+\w+\.json\(\)|\.json\(\)|\w+\.data\b|JSON\.parse\().*/
        || inner.derivedFrom('json', f) || inner.derivedFrom('JSON', f) || inner.derivedFrom('axios', f) || inner.derivedFrom('fetch', f)
    only fromNetwork

    candidate n, "'as' only tells the compiler what the data is: a response or a parsed string can have any shape, so a missing or renamed field fails later, far from here, or is used unchecked. Parse it with a schema (zod, ajv) and use the result",
        n.type == 'as_expression' && !(n.text ==~ /(?s).*\bas\s+(unknown|any|const)\b.*/)
    candidate n, "The ! tells the compiler that this value from the network is never undefined or null: it checks nothing, and a missing field fails later with a confusing error. Validate the data with a schema first",
        n.type == 'non_null_expression'
}
