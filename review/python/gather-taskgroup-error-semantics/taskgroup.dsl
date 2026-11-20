// generated-from: review/python/gather-taskgroup-error-semantics/rule.md
// rule-hash: 35c44df652f70096344e20f6c053b26cf9941c96941e013086c0433688c11294
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'gather-taskgroup-error-semantics'

// A TaskGroup cancels the other tasks when one fails, which can leave a half-done sequence of writes.
with_statement.each { w ->
    only w.child('with_clause') != null && w.child('with_clause').text ==~ /(?s).*TaskGroup\(.*/
    skip file.isTest
    // Non-idempotent writes inside the tasks that the group runs.
    only w.descendants('call').any { k -> k.field('function').text ==~ /(?is)(\w+\.)*(create_task)/ && k.args.any { a -> a.text ==~ /(?is).*(save|write|send|post|put|delete|update|commit|insert|publish|charge|transfer).*/ } }

    candidate w, "If one task of this TaskGroup fails, the others are cancelled where they stand, so non-idempotent writes can be left half done: make them idempotent, or shield them"
}
