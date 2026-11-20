// generated-from: review/java/aws-hardcoded-credentials-or-region/rule.md
// rule-hash: bff715e6efa97625e86270d79b0b84085ae5a94a69cbfd8fac366e590fc8c0e0
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'aws-hardcoded-credentials-or-region'

def literal = { a -> a instanceof StringLit }
def credentialClass = { t -> t in ['BasicAWSCredentials', 'BasicSessionCredentials'] }

// The secret is never quoted in a message.
Creation.each { x ->
    skip file.isTest

    report x, "AWS credentials are written into the source: read them from the default provider chain, the environment or a secret store",
        x.name == 'create' && x.scope.text in ['AwsBasicCredentials', 'AwsSessionCredentials'] && x.args.any(literal)
    report x, "AWS credentials are written into the source: read them from the default provider chain, the environment or a secret store",
        credentialClass(x.typeName) && x.args.any(literal)

    // A literal region or endpoint is a defect only if the environments differ, so the AI decides.
    candidate x, "The AWS region is a literal: a deployment in another region needs a code change",
        x.name == 'of' && x.scope.text == 'Region' && x.args.any(literal)
    candidate x, "The AWS region is a literal: a deployment in another region needs a code change",
        x.name == 'withRegion' && x.args.any(literal)
    candidate x, "The AWS endpoint is a literal: it points every environment at the same service",
        x.name == 'endpointOverride' && x.text ==~ /(?s).*"https?:\/\/.*/
}
