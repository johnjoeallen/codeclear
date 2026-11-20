// generated-from: review/sql/blocking-backfill-or-constraint/rule.md
// rule-hash: a6cd49591b39e3a04b8288520ec9e410093c24dab2db2bcda579f9ca63e4a8c3
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'blocking-backfill-or-constraint'

// How many rows the table has, and how long the lock is held, is not in the file, so these are candidates for the AI.
Statement.each { s ->
    // "Do not flag": a table created in the same file, and a size hint in a comment above.
    skip s.tableCreatedInFile
    skip s.precedingComment ==~ /(?is).*table-size:\s*small.*/

    // One UPDATE over many rows holds locks on all of them until it ends. A LIMIT, TOP, key range or a batch is not one statement over everything.
    candidate s, "This UPDATE changes every matching row in one statement, holding row locks and a long transaction on a table that may be large: batch it by key range",
        s.code ==~ /(?is)\s*UPDATE\b.*\bSET\b.*/ && !(s.code ==~ /(?is).*\b(LIMIT|TOP\s*\(|FETCH\s+FIRST|ROWNUM|BETWEEN|OFFSET)\b.*/) && !(s.code ==~ /(?is).*\b(id|key|pk)\s*(>|<|>=|<=)\s*.*/)
    // "Do not flag": a constraint added NOT VALID and validated separately.
    candidate s, "A foreign key or check constraint added without NOT VALID scans the whole table while holding a lock: add it NOT VALID, then VALIDATE CONSTRAINT",
        s.code ==~ /(?is)\s*ALTER\s+TABLE\b.*\bADD\s+(CONSTRAINT\b.*)?(FOREIGN\s+KEY|CHECK)\b.*/ && !(s.code ==~ /(?is).*\bNOT\s+VALID\b.*/)
    candidate s, "SET NOT NULL scans the whole table while holding a lock: add a NOT VALID check constraint first, validate it, then set NOT NULL",
        s.code ==~ /(?is)\s*ALTER\s+TABLE\b.*\bALTER\s+COLUMN\b.*\bSET\s+NOT\s+NULL\b.*/
    candidate s, "ALTER COLUMN TYPE rewrites the whole table while holding a lock: add a new column, backfill it in batches and switch",
        s.code ==~ /(?is)\s*ALTER\s+TABLE\b.*\bALTER\s+COLUMN\b.*\bTYPE\b.*/ || s.code ==~ /(?is)\s*ALTER\s+TABLE\b.*\b(MODIFY\s+(COLUMN\s+)?\w+\s+\w+|ALTER\s+COLUMN\s+\w+\s+(VARCHAR|NVARCHAR|INT|BIGINT|DECIMAL|NUMERIC)).*/
}
