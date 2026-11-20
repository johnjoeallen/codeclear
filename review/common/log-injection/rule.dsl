// generated-from: review/common/log-injection/rule.md
// rule-hash: fbf77d01c58cf0d89bc06f54fcbc432f848dc5a3cf7157cafac72a519cf9df10
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'log-injection'

// Policy: what is attacker-controlled, and what neutralises a value. The host walks the flow.
def isReqAnn = { a -> a in ['RequestParam', 'PathVariable', 'RequestBody', 'RequestHeader', 'ModelAttribute'] }

def isSource = { n ->
    (n instanceof Call && n.name in ['getParameter', 'getParameterValues', 'getHeader', 'getQueryString',
                                      'getRequestURI', 'getPathInfo'])
        || (n instanceof Param && n.annotations.any(isReqAnn))
}

def isSanitized = { n ->
    n instanceof Call
        && (n.name in ['sanitize', 'sanitise', 'escape', 'encode', 'forLog', 'stripNewlines',
                       'parseInt', 'parseLong', 'parseDouble', 'fromString', 'valueOf']
            || (n.name == 'replaceAll' && n.firstArg.text ==~ /.*\\[rn].*/))
}

Call.each { c ->
    only c.name in ['trace', 'debug', 'info', 'warn', 'error']
        && c.scope != null
        && c.scope.text ==~ /(?i)(this\.)?(log|logger)/
        && !c.args.isEmpty()
    skip file.isTest || project.config('logging.structured') == 'true'

    report c.nameNode, 'Untrusted request value is written to the log without neutralising line breaks',
        c.args.any { a -> a.tainted(isSource, isSanitized) }
}
