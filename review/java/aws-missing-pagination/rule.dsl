// generated-from: review/java/aws-missing-pagination/rule.md
// rule-hash: 20a98284532fa741f127d342a1bcfe98b72da69b017f13296b238a4bbad07b55
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'aws-missing-pagination'

def paginated = { n -> n in ['listObjects', 'listObjectsV2', 'scan', 'query', 'listQueues', 'listTopics', 'listTables', 'listFunctions', 'listBuckets', 'listUsers', 'listRoles', 'listKeys', 'describeInstances', 'describeLogGroups', 'describeLogStreams', 'listStacks', 'listSecrets'] }

// Whether the data can exceed one page is not in the code, so this is a candidate for the AI.
Call.each { c ->
    only paginated(c.name)
    skip file.isTest
    // The call must be on an AWS client: a declared type ending in Client, or an SDK-looking receiver.
    only c.scope.declaredType ==~ /(?i).*(S3|Dynamo|Sqs|Sns|Lambda|Iam|Ec2|CloudWatchLogs|Kms|SecretsManager|CloudFormation|Amazon|Aws)\w*Client(<.*>)?/
    // "Do not flag": a paginator, and a deliberately bounded read.
    skip c.name ==~ /.*Paginator/
    skip c.text ==~ /(?s).*\.(maxKeys|limit|maxResults|maxItems)\(\s*\d+\s*\).*/
    // A loop over the continuation token in the same method.
    skip c.enclosingMethod != null && c.enclosingMethod.text ==~ /(?s).*(nextContinuationToken|lastEvaluatedKey|nextToken|NextToken|isTruncated|getNextMarker|nextMarker).*/

    candidate c, "${c.name}() returns one page and the code never asks for the next: results past the first page are silently missing, so use the paginator or loop on the continuation token"
}
