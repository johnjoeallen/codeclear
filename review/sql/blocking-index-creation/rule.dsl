// generated-from: review/sql/blocking-index-creation/rule.md
// rule-hash: 351aeaeb9db9cce689c08697af49baee477c5d0811e832d7b579efe195cc5c46
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'blocking-index-creation'

// How long the build takes depends on the table size, so this is a candidate for the AI.
Statement.each { s ->
    // "Do not flag": an index on a table created in the same file, and a size hint in a comment above.
    skip s.tableCreatedInFile
    skip s.precedingComment ==~ /(?is).*table-size:\s*small.*/

    candidate s, "CREATE INDEX without CONCURRENTLY locks the table against writes while the index is built: use CREATE INDEX CONCURRENTLY",
        s.code ==~ /(?is)\s*CREATE\s+(UNIQUE\s+)?(CLUSTERED\s+|NONCLUSTERED\s+)?INDEX\b.*/ && !(s.code ==~ /(?is).*\bCONCURRENTLY\b.*/) && !(s.code ==~ /(?is).*ONLINE\s*=\s*ON.*/)
    candidate s, "ADD INDEX without ALGORITHM=INPLACE, LOCK=NONE can copy the table and block writes while it runs",
        s.code ==~ /(?is)\s*ALTER\s+TABLE\b.*\bADD\s+(UNIQUE\s+)?(INDEX|KEY)\b.*/ && !(s.code ==~ /(?is).*ALGORITHM\s*=\s*(INPLACE|INSTANT).*LOCK\s*=\s*NONE.*|.*LOCK\s*=\s*NONE.*ALGORITHM\s*=\s*(INPLACE|INSTANT).*/)
}
