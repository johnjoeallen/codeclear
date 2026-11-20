// generated-from: review/java/kafka-producer-send-unobserved/rule.md
// rule-hash: 6de0733c35dcd5138288e22b3924dec8e630054973abd1a374727fc8d5dfa13a
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'kafka-producer-send-unobserved'

// A send whose result is thrown away: it is a statement of its own, so there is no .get(), no whenComplete and nothing is
// stored or returned. Whether fire-and-forget is acceptable here is for the AI to judge.
Call.each { c ->
    only c.name in ['send', 'sendDefault'] && c.scope.text ==~ /(?i).*(kafka|producer|template).*/ && c.isDiscarded
    skip file.isTest
    // "Do not flag": a callback is passed, as in producer.send(record, (metadata, error) -> ...).
    skip c.args.any { a -> a instanceof Lambda || a instanceof MethodRef || a instanceof New }

    candidate c, "The result of ${c.scope.text}.${c.name}() is discarded: a failed send is never noticed"
}
