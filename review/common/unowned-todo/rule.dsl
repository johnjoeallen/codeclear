// generated-from: review/common/unowned-todo/rule.md
// rule-hash: b307e1218ef7b985273ca46e3451f0f514315ae12308dd10e2f72c3e5f5c96e1
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'unowned-todo'

// Every comment in the file; only lines the change touches are reported, so a marker that was already there is not.
Comment.each { c ->
    only c.text ==~ /(?s).*\b(TODO|FIXME|HACK|XXX)\b.*/
    // "Do not flag": a marker with an owner, TODO(jane), or a ticket: PAY-1234, #123 or an issue URL.
    skip c.text ==~ /(?s).*\b(TODO|FIXME|HACK|XXX)\s*\([^)]+\).*/
    skip c.text ==~ /(?s).*([A-Z][A-Z0-9]+-\d+|#\d+|https?:\/\/\S+).*/

    // Reported on the line of the marker itself, and quoting it, so that a long comment is not tagged at its first line.
    report c.marker, "This TODO has no owner and no ticket: write TODO(name) or reference an issue (${c.markerText})"
}
