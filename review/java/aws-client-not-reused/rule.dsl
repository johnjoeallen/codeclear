// generated-from: review/java/aws-client-not-reused/rule.md
// rule-hash: eb3211621f8f1365cc27fac2c404b0a83000ff946ff91049a9445a76f94a88a9
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'aws-client-not-reused'

// Other libraries have classes called ...Client with a create() or builder(); only the AWS service clients are meant.
def notAws = { t -> t ==~ /(Http|Web|Rest|OkHttp|Feign|Grpc|Kafka|Redis|Mongo|Elasticsearch|Jdbc|Ftp|Smtp|Rabbit|Cassandra|Es)\w*Client\w*/ }
def notAwsText = { t -> t ==~ /(?s)(Http|Web|Rest|OkHttp|Feign|Grpc|Kafka|Redis|Mongo|Elasticsearch|Jdbc|Ftp|Smtp|Rabbit|Cassandra|Es)\w*Client.*/ }
def clientType = { t -> t ==~ /[A-Z]\w*Client(Builder)?/ && !notAws(t) }
def perCreation = { m -> m.annotations.any { a -> a in ['Bean', 'PostConstruct'] } }

// The creation expression: S3Client.create(), AmazonSQSClientBuilder.defaultClient(), or the build() ending
// S3Client.builder()...build() or AmazonS3ClientBuilder.standard()...build().
Call.each { c ->
    only c.name in ['create', 'defaultClient'] && c.args.isEmpty() && clientType(c.scope.text)
        || c.name == 'build' && c.args.isEmpty() && c.text ==~ /(?s)[A-Z]\w*Client(Builder)?\s*\.\s*(builder|standard)\(\).*/ && !notAwsText(c.text)
    // "Do not flag": constructors, static and field initialisers (no enclosing method), @Bean and @PostConstruct methods, tests.
    only c.enclosingMethod != null
    skip file.isTest || perCreation(c.enclosingMethod)
    // Stored in a field or passed on: only a client held in a local variable is created per call.
    only c.assignedToLocal

    report c, "A new AWS client is built on every call: it owns a connection pool and threads, so create it once and reuse it",
        !c.inTryResource || c.inside(Loop), confidence: c.inTryResource ? LOW : DEFAULT
}
