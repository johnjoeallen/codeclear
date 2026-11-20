// generated-from: review/typescript/tls-verification-disabled/rule.md
// rule-hash: 429bc707cef34790e14f0721e818c06e0c65f5e270339ee843e36d1d6a1f5b68
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'tls-verification-disabled'


Node.each { n ->
    only n.type in ['pair', 'assignment_expression']
    // "Do not flag": tests and fixtures, and a block bound to NODE_ENV === 'development'.
    skip file.isTest || file.path ==~ /(?i).*(fixtures?|mocks?)\/.*/
    def guard = n.ancestor('if_statement')
    skip guard != null && guard.field('condition').text ==~ /(?s).*NODE_ENV\s*===?\s*["']development["'].*/

    report n, "rejectUnauthorized: false turns certificate verification off, so the connection can be intercepted",
        n.type == 'pair' && n.field('key').text ==~ /["']?rejectUnauthorized["']?/ && n.field('value').type == 'false'
    report n, "strictSSL: false turns certificate verification off, so the connection can be intercepted",
        n.type == 'pair' && n.field('key').text ==~ /["']?strictSSL["']?/ && n.field('value').type == 'false'
    report n, "checkServerIdentity that returns undefined accepts any server name, so the connection can be intercepted",
        n.type == 'pair' && n.field('key').text ==~ /["']?checkServerIdentity["']?/
            && n.field('value').type in ['arrow_function', 'function_expression'] && n.field('value').field('body').type == 'undefined'
    report n, "NODE_TLS_REJECT_UNAUTHORIZED = 0 turns certificate verification off for the whole process",
        n.type == 'assignment_expression' && n.field('left').text == 'process.env.NODE_TLS_REJECT_UNAUTHORIZED'
            && n.field('right').text in ["'0'", '"0"', '0']
}
