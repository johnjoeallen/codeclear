// generated-from: review/sql/update-or-delete-without-where/rule.md
// rule-hash: 96aaa4551e23c8a2d77d3ea3555fd35a19758776b3f86360f02f8c58b05cac0d
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'update-or-delete-without-where'

Statement.each { s ->
    only s.code ==~ /(?is)\s*(UPDATE\b.*\bSET\b|DELETE\s+FROM\b).*/
    // A WHERE outside any subquery; and an UPDATE ... FROM that joins on a condition.
    skip s.hasTopLevelWhere
    skip s.code ==~ /(?is).*\bFROM\b.*\bJOIN\b.*\bON\b.*/
    // "Do not flag": a table created in the same file, and the explicit opt-out comment.
    skip s.tableCreatedInFile
    skip s.precedingComment ==~ /(?is).*full-table:\s*intentional.*/

    report s, "This statement has no WHERE clause, so it changes every row: add a condition, or write '-- full-table: intentional' above it"
}
