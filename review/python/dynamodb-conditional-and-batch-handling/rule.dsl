// generated-from: review/python/dynamodb-conditional-and-batch-handling/rule.md
// rule-hash: e36e930c03b729b5c13ca105dc64c20527cab38de9e17df9734c7372bf330f46
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'dynamodb-conditional-and-batch-handling'

call.each { c ->
    skip file.isTest
    def module = c.ancestor('module')
    only module.mentions('boto3') || module.mentions('dynamodb') || module.mentions('aioboto3')
    def fn = c.field('function')
    def name = fn.type == 'attribute' ? fn.field('attribute').text : fn.text
    def scope = c.ancestorOf(['function_definition', 'module'])
    def item = c.keyword('Item')

    // "Do not flag": batch_writer(), which retries unprocessed items itself, and a function that reads the unprocessed list.
    candidate c, "${name}() can return some items unprocessed (throttling, size limits) and this code never looks at UnprocessedItems or UnprocessedKeys, so those writes or reads are silently dropped. Use batch_writer(), or loop until the list is empty",
        name in ['batch_write_item', 'batch_get_item'] && !(scope.text ==~ /(?s).*Unprocessed(Items|Keys).*/)
    // A Python float in an item: boto3 refuses it, and converting it loses precision, so Decimal is the type to use.
    candidate c, "The item has a float: DynamoDB numbers must be Decimal, and a float raises TypeError (or is rounded by a conversion). Use Decimal(str(x))",
        name in ['put_item', 'update_item', 'batch_write_item'] && item != null && !item.descendants('float').isEmpty()
    // A read straight after a write in the same function is eventually consistent unless it asks otherwise.
    candidate c, "${name}() reads right after a write in this function, but a default read is eventually consistent and can return the old item: pass ConsistentRead=True",
        name in ['get_item', 'query'] && (scope.mentions('put_item') || scope.mentions('update_item') || scope.mentions('delete_item')) && (c.keyword('ConsistentRead') == null || c.keyword('ConsistentRead').type == 'false')
}
