// generated-from: review/sql/add-not-null-column-without-default/rule.md
// rule-hash: f021bc8041068334c8c634ccfd087cfe5d51cae2b31936201e83c60a0b5fa7a8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'add-not-null-column-without-default'

// A column definition added by ALTER TABLE ... ADD [COLUMN], one per column of a multi-column ADD.
Column.each { c ->
    only c.code ==~ /(?is).*\bNOT\s+NULL\b.*/
    // "Do not flag": a default, and a value the database generates.
    skip c.code ==~ /(?is).*\b(DEFAULT|GENERATED|IDENTITY|SERIAL|BIGSERIAL|SMALLSERIAL|AUTO_INCREMENT|AUTOINCREMENT)\b.*/
    // "Do not flag": a table created in the same file is empty.
    skip c.tableCreatedInFile

    report c, "The NOT NULL column ${c.name} has no DEFAULT: the ALTER fails on a table with rows, and old code that does not write it fails its inserts during a rolling deploy"
}
