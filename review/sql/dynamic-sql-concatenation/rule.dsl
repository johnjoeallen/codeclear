// generated-from: review/sql/dynamic-sql-concatenation/rule.md
// rule-hash: d0c7fe12f8dd26ef64853fd65c22f17e3e32f6df68b6dac86852ed3eac5af853
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'dynamic-sql-concatenation'

// Lines of code, with comments removed and the contents of strings blanked ('code'), so a + inside a string is not seen.
Line.each { l ->
    // "Do not flag": sp_executesql with parameters, a USING binding, and format() with %I and %L (quote_ident / quote_literal).
    skip l.code ==~ /(?is).*\bsp_executesql\b.*/
    skip l.text ==~ /(?is).*\bformat\s*\(.*/ || l.text ==~ /(?is).*\b(quote_ident|quote_literal|quote_nullable)\s*\(.*/
    skip l.code ==~ /(?is).*\bUSING\b.*/

    report l, "EXEC builds its command by concatenating a variable: pass the value as a parameter (sp_executesql) instead",
        l.code ==~ /(?is).*\bEXEC(UTE)?\s*\(.*(\+\s*@\w+|@\w+\s*\+).*/
    report l, "The executed string is built by concatenation: pass the values as parameters (sp_executesql) instead",
        l.executesBuiltString
    report l, "EXECUTE IMMEDIATE concatenates a variable into the command: bind it with USING instead",
        l.code ==~ /(?is).*\bEXECUTE\s+IMMEDIATE\b.*\|\|\s*[A-Za-z_@:].*/
    report l, "EXECUTE concatenates a variable into the command: use format('%I'/'%L') or USING instead",
        l.code ==~ /(?is).*\bEXECUTE\b(?!\s+IMMEDIATE).*\|\|\s*[A-Za-z_@:].*/
}
