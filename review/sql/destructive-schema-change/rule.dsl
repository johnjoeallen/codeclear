// generated-from: review/sql/destructive-schema-change/rule.md
// rule-hash: 50c3ab4230cb5d9f54d87e25563135e52ff5243e736ec63bade25f472117ce74
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'destructive-schema-change'

// Whether the data can be lost or the old code breaks depends on the data and on the deployment, so these are candidates for the AI.
Statement.each { s ->
    // "Do not flag": a rollback migration, temporary or staging tables, and a table created in the same file.
    skip file.path ==~ /(?i).*(\/|^)(down|rollback|undo)[\/_.\-].*|.*[._\-](down|rollback|undo)\.sql/
    skip s.code ==~ /(?is).*\b(tmp_|temp_)\w*.*/
    skip s.tableCreatedInFile

    candidate s, "DROP TABLE deletes the table and its data: old application code that still reads it fails, and the data is gone",
        s.code ==~ /(?is)\s*DROP\s+TABLE\b.*/
    candidate s, "TRUNCATE deletes every row of the table: the data cannot be recovered without a backup",
        s.code ==~ /(?is)\s*TRUNCATE\b.*/
    candidate s, "DROP COLUMN deletes the column and its data, and old application code that still reads it fails during a rolling deploy",
        s.code ==~ /(?is)\s*ALTER\s+TABLE\b.*\bDROP\s+(COLUMN\b|\w+\s*(,|;|$)).*/ && !(s.code ==~ /(?is).*\bDROP\s+(CONSTRAINT|INDEX|PRIMARY|FOREIGN|DEFAULT).*/)
    candidate s, "Renaming a table or column breaks old application code that still uses the old name during a rolling deploy",
        s.code ==~ /(?is)\s*ALTER\s+TABLE\b.*\bRENAME\b.*/ || s.code ==~ /(?is)\s*(RENAME\s+TABLE|EXEC(UTE)?\s+sp_rename)\b.*/
    candidate s, "Changing the type of a column can truncate or reject existing values, and rewrites the table: check that it does not narrow",
        s.code ==~ /(?is)\s*ALTER\s+TABLE\b.*\b(ALTER\s+COLUMN\b.*\bTYPE\b|ALTER\s+COLUMN\s+\w+\s+(VARCHAR|CHAR|INT|BIGINT|SMALLINT|DECIMAL|NUMERIC|TEXT)|MODIFY\b|CHANGE\s+COLUMN\b).*/
    candidate s, "Dropping a constraint removes a guarantee that the data and old code rely on",
        s.code ==~ /(?is)\s*ALTER\s+TABLE\b.*\bDROP\s+(CONSTRAINT|FOREIGN\s+KEY|PRIMARY\s+KEY|CHECK)\b.*/
}
