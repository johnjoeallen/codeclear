// generated-from: review/java/timezone-ambiguity/rule.md
// rule-hash: 4c0406f21799d5bdc32f4f6ff6bdab84a85953d1108a33f5fd848dcf1c51686b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'timezone-ambiguity'

New.each { n ->
    skip file.isTest

    // A SimpleDateFormat formats and parses in the machine's time zone unless setTimeZone is called on it.
    candidate n, "This SimpleDateFormat uses the machine's time zone: call setTimeZone",
        n.typeName == 'SimpleDateFormat' && (n.enclosingMethod == null || !n.enclosingMethod.calls.any { x -> x.name == 'setTimeZone' })
}
