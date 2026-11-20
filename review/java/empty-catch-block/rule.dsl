// generated-from: review/java/empty-catch-block/rule.md
// rule-hash: f048993f9cff87c64bd101e2a389c4cb36615f2ab41d78c2a934d89510222587
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'empty-catch-block'

// A statement that is only a DEBUG/TRACE log call: log.debug(...), logger.trace(...), LOG.debug(...)
def quiet = { s ->
    s instanceof ExprStmt
        && s.expression instanceof Call
        && s.expression.name in ['debug', 'trace']
        && s.expression.scope.text ==~ /(?i)(this\.)?log(ger)?/
}

Catch.each { c ->
    // "Do not flag": test sources, an ignored/ignore/expected parameter, an explaining comment.
    skip file.isTest || c.param.name in ['ignored', 'ignore', 'expected'] || c.body.hasComment

    // The first report whose condition holds fires: a catch gets at most one finding.
    report c, "Empty catch block hides ${c.param.type} from the caller",
        c.body.statements.isEmpty()
    report c, "Catch block only logs at DEBUG/TRACE, hiding ${c.param.type} from the caller",
        c.body.statements.every(quiet)
}
