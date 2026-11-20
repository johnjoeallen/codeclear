// generated-from: review/java/scheduled-task-overlap/rule.md
// rule-hash: 854ccbf756146fdf611e61f08981e057f5d546c25bf5dc686a3754db4c6b9993
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'scheduled-task-overlap'

def remote = { t -> t ==~ /.*(RestTemplate|RestClient|WebClient|HttpClient|Feign.*|KafkaTemplate|RabbitTemplate|JmsTemplate|Sqs.*Client|Sns.*Client|S3Client|JdbcTemplate)(<.*>)?/ }
def lock = { a -> a in ['SchedulerLock', 'Lock'] }

// How long the job runs against its period, and how many instances run it, is not in the file, so this is a candidate for the AI.
Annotation.each { a ->
    only a.name == 'Scheduled' && a.text ==~ /(?s).*fixedRate.*/
    skip file.isTest
    def m = a.enclosingMethod
    only m != null

    // "Do not flag": a job that holds a distributed lock.
    skip m.annotations.any(lock)
    // A job that takes long or unbounded time: a remote call, a sleep, a findAll or a loop.
    only m.calls.any { x -> x.name == 'sleep' || x.name == 'findAll' || remote(x.scope.declaredType) } || m.text ==~ /(?s).*\b(for|while)\s*\(.*/

    candidate a, "${m.name}() runs at a fixed rate and does remote or unbounded work with no lock: runs can overlap, starve other scheduled tasks or repeat on every instance"
}
