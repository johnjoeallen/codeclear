// generated-from: review/java/shared-simpledateformat/rule.md
// rule-hash: 168188505c42940bf8826a0610dbcd4629c27bc718fa723578c92b403fd2fa97
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'shared-simpledateformat'

def singleton = { a -> a in ['Component', 'Service', 'Repository', 'Controller', 'RestController', 'Configuration'] }

// Only field declarations are visited, so local variables are never flagged. A ThreadLocal<SimpleDateFormat> has the
// type ThreadLocal and does not match.
FieldDecl.each { f ->
    only f.typeName in ['SimpleDateFormat', 'DateFormat'] || f.initializerTypeName == 'SimpleDateFormat'
    only f.isStatic || f.classAnnotations.any(singleton)

    // A private final field that is only ever used inside synchronized code is much less likely to be a bug.
    report f, "A shared ${f.typeName} is not thread-safe; use java.time.DateTimeFormatter",
        confidence: f.usedOnlyInSynchronized ? LOW : DEFAULT
}
