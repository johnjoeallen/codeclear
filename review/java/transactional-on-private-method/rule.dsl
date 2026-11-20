// generated-from: review/java/transactional-on-private-method/rule.md
// rule-hash: 296cdf25c2b607b8716cddb194ecefcdd7b620dafefd892fe6ab87508d34d5b6
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'transactional-on-private-method'

def isTransactional = { a -> a == 'Transactional' }
// Classes Spring creates a proxy for.
def isBean = { a -> a in ['Service', 'Component', 'Repository', 'Controller', 'RestController', 'Configuration', 'Transactional'] }

Method.each { m ->
    only m.annotations.any(isTransactional)
    // "Do not flag": AspectJ weaving, which does not use a proxy (the project configuration says so).
    skip project.config('transaction.mode') == 'aspectj'

    report m.nameNode, "@Transactional on a private method is ignored: Spring's proxy cannot intercept it", m.isPrivate
    report m.nameNode, "@Transactional on a final method is ignored: the CGLIB proxy cannot override it", m.isFinal && m.enclosingClassAnnotations.any(isBean)
}
