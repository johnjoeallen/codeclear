// generated-from: review/python/nosql-operator-injection/rule.md
// rule-hash: 06f7c8f2b4cd289816dbdf7a43739f819d0a8ab26f11deaa3e05184076ab191b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'nosql-operator-injection'

def queryMethod = { n -> n in ['find', 'find_one', 'update_one', 'update_many', 'delete_one', 'delete_many', 'count_documents', 'aggregate', 'replace_one', 'find_one_and_update', 'find_one_and_delete', 'find_one_and_replace', 'distinct'] }

// A filter built from the request body can carry operators ({"$ne": null}, {"$gt": ""}) that match every document.
call.each { c ->
    skip file.isTest
    def fn = c.field('function')
    def name = fn.type == 'attribute' ? fn.field('attribute').text : fn.text
    def module = c.ancestor('module')
    only module.mentions('pymongo') || module.mentions('motor') || module.mentions('mongoengine') || module.mentions('motor_asyncio')
    def scope = c.ancestorOf(['function_definition', 'module'])
    // "Do not flag": a value turned into a string (str(...)) or checked against a schema.
    def validated = scope.text ==~ /(?is).*(schema|validate|pydantic|marshmallow).*/

    candidate c, "A query filter is built from request data: a body like {\"$ne\": null} is read as an operator and matches every document. Convert each value with str(), or validate the body against a schema first",
        (queryMethod(name) || name == 'objects') && c.args.any { a -> a.derivedFrom('request', scope) && !(a.text ==~ /(?s)\s*str\(.*/) && !a.descendants('call').any { k -> k.field('function').text == 'str' } } && !validated
}
