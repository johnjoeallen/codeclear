// generated-from: review/java/generic-exception-in-public-api/rule.md
// rule-hash: c435fb22e2543a76668a88b941738d61bee557cbda5f1bb1f6d4cd55c8d3611c
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'generic-exception-in-public-api'

// throw new RuntimeException(...), new Exception(...) or new Throwable(...) in a public method of a public type.
def isFrameworkEntry = { a -> a in ['Bean', 'Around', 'Before', 'After', 'AfterReturning', 'AfterThrowing', 'PostConstruct', 'PreDestroy', 'Test', 'BeforeEach', 'AfterEach', 'BeforeAll', 'AfterAll'] }

Throw.each { t ->
    only t.expression.typeName in ['RuntimeException', 'Exception', 'Throwable']
    def m = t.enclosingMethod
    only m != null && m.isPublic && m.classIsPublic
    skip 'Override' in m.annotations || m.annotations.any(isFrameworkEntry) || m.name in ['main', 'run', 'call', 'close', 'afterPropertiesSet', 'destroy'] || file.isTest

    candidate t, "Public method ${m.name}() throws a generic ${t.expression.typeName}: callers cannot handle the failure separately"
}
