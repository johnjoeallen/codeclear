// generated-from: review/java/retry-without-bound-or-idempotency/rule.md
// rule-hash: 25db52bafd86b0b15540a17cf3ac01cdbd88de6dabad55025c601f24ae0b906e
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'retry-without-bound-or-idempotency'

Annotation.each { a ->
    only a.name == 'Retryable'
    skip file.isTest
    // "Do not flag": retries with an explicit bound and a backoff.
    candidate a, "@Retryable names no maxAttempts, so it uses the default and retries every failure alike: set maxAttempts and a backoff",
        !(a.text ==~ /(?s).*maxAttempts.*/) && !(a.text ==~ /(?s).*backoff.*/)
    candidate a, "@Retryable retries on Exception or Throwable, so it also retries bugs and permanent failures: list the transient exceptions only",
        a.text ==~ /(?s).*(include|retryFor|value)\s*=\s*\{?\s*(java\.lang\.)?(Exception|Throwable)\.class.*/
}
