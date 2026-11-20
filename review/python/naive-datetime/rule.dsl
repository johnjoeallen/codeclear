// generated-from: review/python/naive-datetime/rule.md
// rule-hash: 43663d7c8bff440606f98ead1073861bd51237d6e97c98992b18b0261b60dc50
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'naive-datetime'

def datetimeClass = { t -> t in ['datetime', 'datetime.datetime'] }

// A datetime with no timezone is local time of whichever machine runs the code.
call.each { c ->
    only c.field('function').type == 'attribute' && datetimeClass(c.field('function').field('object').text)
    // "Do not flag": local-only scripts and test files.
    skip file.isTest
    def method = c.field('function').field('attribute').text
    def tz = c.keyword('tz') != null || c.keyword('tzinfo') != null

    report c, "datetime.now() without a timezone gives the local time of the machine: pass one, datetime.now(timezone.utc)",
        method == 'now' && !tz && c.positional.isEmpty()
    report c, "datetime.utcnow() gives a naive datetime and is deprecated since Python 3.12: use datetime.now(timezone.utc)",
        method == 'utcnow'
    report c, "datetime.utcfromtimestamp() gives a naive datetime and is deprecated since Python 3.12: use datetime.fromtimestamp(x, timezone.utc)",
        method == 'utcfromtimestamp'
    report c, "datetime.fromtimestamp() without a timezone uses the local one: pass tz=timezone.utc",
        method == 'fromtimestamp' && !tz && c.positional.size().isEqualTo(1)
}
