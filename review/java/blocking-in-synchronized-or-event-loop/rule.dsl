// generated-from: review/java/blocking-in-synchronized-or-event-loop/rule.md
// rule-hash: cd87247183b5ef48dcd49377fbadfd934bf9e0c40515e645aedb028630413b6a
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'blocking-in-synchronized-or-event-loop'

def remote = { t -> t ==~ /.*(RestTemplate|RestClient|WebClient|HttpClient|JdbcTemplate|Feign.*|.*Repository|Sqs.*Client|S3Client)(<.*>)?/ }
def blockingName = { n -> n in ['sleep', 'join', 'take', 'block', 'blockFirst', 'blockLast'] }

// Which calls block, and for how long, depends on the callee, so this is a candidate for the AI.
Call.each { x ->
    skip file.isTest
    // "Do not flag": blocking on a dedicated scheduler or executor.
    def m = x.enclosingMethod
    skip m != null && m.text ==~ /(?s).*(boundedElastic|subscribeOn\s*\()+.*/

    candidate x, "${x.name}() blocks while a lock is held: other threads wait for the lock for the whole call",
        x.inSynchronized && (blockingName(x.name) || (x.name == 'get' && x.args.isEmpty()) || remote(x.scope.declaredType)
            || x.scope.text == 'Files')
    candidate x, "${x.name}() blocks inside a reactive or asynchronous stage: it ties up an event-loop or shared-pool thread",
        x.name in ['block', 'blockFirst', 'blockLast', 'join'] && x.inside(Lambda) && !x.inSynchronized
}
