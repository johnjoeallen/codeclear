// generated-from: review/java/generic-exception-in-public-api/rule.md
// rule-hash: c435fb22e2543a76668a88b941738d61bee557cbda5f1bb1f6d4cd55c8d3611c
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'generic-exception-in-public-api'

// A public method of a public type that declares throws Exception or Throwable. Whether callers need to tell the
// failures apart depends on how the API is used, so this is a candidate for the AI to confirm.
// Methods whose signature the framework or language dictates: advice, bean factories, lifecycle callbacks, test hooks.
def isFrameworkEntry = { a -> a in ['Bean', 'Around', 'Before', 'After', 'AfterReturning', 'AfterThrowing', 'PostConstruct', 'PreDestroy', 'Test', 'BeforeEach', 'AfterEach', 'BeforeAll', 'AfterAll'] }

Method.each { m ->
    only m.isPublic && m.classIsPublic && m.throwsTypes.any { t -> t in ['Exception', 'Throwable', 'java.lang.Exception', 'java.lang.Throwable'] }
    // "Do not flag": overrides (they must match the method they implement), framework entry points, main, tests, and anything that is not public.
    skip 'Override' in m.annotations || m.annotations.any(isFrameworkEntry) || m.name in ['main', 'run', 'call', 'close', 'afterPropertiesSet', 'destroy'] || file.isTest

    candidate m.nameNode, "Public method ${m.name}() declares a generic exception: callers cannot handle its failures separately"
}
