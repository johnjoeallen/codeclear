// generated-from: review/java/aws-overbroad-iam-policy/rule.md
// rule-hash: 2890e71934fe5788225f887be38ae594063e6608e0ff2dd985b9c8c59ba9e299
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'aws-overbroad-iam-policy'

def allowedWildcardResource = { s -> s ==~ /(?s).*(sts:GetCallerIdentity|logs:CreateLogGroup|logs:CreateLogStream|logs:PutLogEvents|xray:Put\w*\*?|xray:Get\w*).*/ }

StringLit.each { t ->
    skip file.isTest
    def calls = t.enclosingCalls
    def statement = t.enclosingStatement

    // An action that is "*" or "<service>:*", in a CDK builder or in a JSON policy string.
    candidate t, "This IAM policy allows every action of a service (or all of them): list only the actions that are needed",
        t.text ==~ /"(\*|[A-Za-z0-9-]+:\*)"/ && calls.any { n -> n in ['actions', 'addActions', 'withActions', 'action'] }
    candidate t, "This policy document allows every action of a service (or all of them): list only the actions that are needed",
        t.text ==~ /(?s)".*\\?"Action\\?"\s*:\s*(\[\s*)?\\?"(\*|[A-Za-z0-9-]+:\*)\\?".*"/
    // Resource "*" together with actions that work on data, unless every action is one that requires "*".
    candidate t, "This policy applies to every resource ('*'): name the resources, unless the actions need '*'",
        t.text == '"*"' && calls.any { n -> n in ['resources', 'addResources', 'withResources', 'resource'] } && !allowedWildcardResource(statement.text)
    candidate t, "This policy document applies to every resource ('*'): name the resources, unless the actions need '*'",
        t.text ==~ /(?s)".*\\?"Resource\\?"\s*:\s*\\?"\*\\?".*"/ && !allowedWildcardResource(t.text)
}
