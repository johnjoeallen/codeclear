// generated-from: review/typescript/nosql-operator-injection/rule.md
// rule-hash: fa04edec9825962e32fb80bc74380453527280a867b3bed3a874275e5c494905
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'nosql-operator-injection'

def queryMethod = { n -> n in ['find', 'findOne', 'findOneAndUpdate', 'findOneAndDelete', 'findOneAndReplace', 'updateOne', 'updateMany', 'deleteOne', 'deleteMany', 'aggregate', 'where', 'countDocuments', 'replaceOne', 'distinct'] }
def coerce = { k -> k.field('function').text in ['String', 'Number', 'parseInt', 'parseFloat', 'Boolean', 'toString'] }

// A filter built from the request body can carry operators ({"$ne": null}) that match every document.
call_expression.each { c ->
    skip file.isTest
    def module = c.ancestor('program')
    only module.text ==~ /(?s).*(mongoose|mongodb|mongo-|Mongo).*/
    def fn = c.field('function')
    only fn.type == 'member_expression' && queryMethod(fn.field('property').text)
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    // "Do not flag": a schema or validator in the function, express-mongo-sanitize, and values cast to a string or number.
    skip f.text ==~ /(?s).*(mongoSanitize|mongo-sanitize|sanitize|zod|Joi|yup|ajv|validate|safeParse|celebrate|[sS]chema|\.parse\().*/ || module.text ==~ /(?s).*(express-mongo-sanitize|mongoSanitize).*/

    candidate c, "A query filter is built from request data: a body such as {\"password\": {\"$ne\": null}} is read as an operator and matches every document. Cast each value (String(x)), or validate the body with a schema first",
        c.args.any { a -> (a.derivedFrom('req', f) || a.derivedFrom('request', f)) && !a.descendants('call_expression').any(coerce) && !(a.type == 'call_expression' && coerce(a)) }
}
