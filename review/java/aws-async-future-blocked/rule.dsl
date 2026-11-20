// generated-from: review/java/aws-async-future-blocked/rule.md
// rule-hash: 3d9feed0b208152447bcc5509ead4e50ad14423ba5ddc6e282896b9ee90f1278
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'aws-async-future-blocked'

// The chain: client.operation(...).join() or .get(): the call before it returns the future.
Call.each { c ->
    only c.name in ['join', 'get'] && c.args.isEmpty()
    skip file.isTest
    only c.scope instanceof Call && c.scope.scope.declaredType ==~ /(\w*\.)*\w*AsyncClient(<.*>)?/
    // "Do not flag": blocking at an application boundary (main, startup) and on a dedicated blocking executor.
    def m = c.enclosingMethod
    skip m != null && (m.name in ['main', 'run'] || m.annotations.any { a -> a in ['PostConstruct', 'Bean', 'EventListener'] })
    skip c.enclosingMethodName == 'main'

    candidate c, "${c.scope.name}() is asynchronous, but ${c.name}() waits for it right away: the thread blocks as if the call were synchronous. Return the future, or use the blocking client"
}
