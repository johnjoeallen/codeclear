// generated-from: review/java/retry-without-bound-or-idempotency/rule.md
// rule-hash: 25db52bafd86b0b15540a17cf3ac01cdbd88de6dabad55025c601f24ae0b906e
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'retry-without-bound-or-idempotency'

def unbounded = { l -> l.text ==~ /(?s)\s*(while\s*\(\s*true\s*\)|for\s*\(\s*;\s*;\s*\)).*/ }
def counted = { l -> l.text ==~ /(?is).*(attempt|retries|retry|tries|deadline|maxwait|timeout|nanoTime|currentTimeMillis|Instant\.now).*/ }

// A catch inside a loop that goes round again: that is a retry.
Catch.each { c ->
    skip file.isTest
    def l = c.enclosingLoop
    only l != null
    // The catch carries on: it does not leave the loop or the method.
    only c.body.statements.every { s -> !(s instanceof Jump) }
    // "Do not flag": a loop bounded by a counter or a deadline with a pause between attempts.
    def pause = l.text ==~ /(?is).*(sleep|backoff|delay|await|\.wait\(|schedule|TimeUnit|Thread\.onSpinWait).*/
    def write = l.text ==~ /(?s).*\.(post|put|save|send|charge|publish|putItem|insert|create|execute)\w*\(.*/
    def keyed = l.text ==~ /(?is).*(idempotency|idempotent|requestId|conditionExpression|ifNotExists|attribute_not_exists|etag|if-match).*/

    candidate c, "This retry loop has no attempt counter or deadline, so a permanent failure retries forever",
        unbounded(l) && !counted(l)
    candidate c, "This retry loop pauses for nothing between attempts, so it hammers a failing service: wait with a growing delay",
        !pause
    // "Do not flag": retries around reads, or around a write that carries an idempotency key.
    candidate c, "This retries a write: if the first attempt succeeded but the answer was lost, it is done twice. Use an idempotency key or a conditional write",
        write && !keyed
}
