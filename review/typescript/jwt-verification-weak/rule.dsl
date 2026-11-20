// generated-from: review/typescript/jwt-verification-weak/rule.md
// rule-hash: 14064c99b7663603ea1575a4d132cc37cbd285056ed5bf8e7c27d67c74ce0423
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'jwt-verification-weak'

def functionTypes = ['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program']
def keyed = { o -> o != null && o.type == 'object' }
def hasIgnoreExpiration = { o -> keyed(o) && o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?ignoreExpiration["']?/ && p.field('value').type == 'true' } }
def hasAlgorithms = { o -> keyed(o) && o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?algorithms["']?/ } }
def algorithmsNone = { o -> keyed(o) && o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?algorithms["']?/ && p.field('value').text ==~ /(?is).*["']none["'].*/ } }
def hasIssuerOrAudience = { o -> keyed(o) && o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?(issuer|audience)["']?/ } }
// An options value we cannot see into: a variable or a spread may hold the setting, so it is not flagged.
def opaque = { o -> o != null && (o.type == 'identifier' || (keyed(o) && o.children.any { p -> p.type == 'spread_element' })) }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    def opts = c.arg(2)
    def scope = c.ancestorOf(functionTypes)
    def module = c.ancestor('program')
    // jwt.decode(...) checks nothing: it is a problem only when its result decides who the caller is.
    def holder = c.parent != null && c.parent.type == 'await_expression' ? c.parent.parent : c.parent
    def v = holder != null && holder.type == 'variable_declarator' ? holder.field('name') : holder

    report c, "jwt.decode() does not check the signature, and its result is used to decide who the caller is: use jwt.verify() with pinned algorithms",
        fn ==~ /(jwt|jsonwebtoken)\.decode/ && holder != null
            && ((holder.type == 'assignment_expression' && holder.field('left').text ==~ /(req|request|ctx|res\.locals|session|context)\..*/)
                || holder.type == 'return_statement'
                || (holder.type == 'variable_declarator' && v.type == 'identifier' && scope.countOf(v.text).isGreaterThan(1)
                    && (scope.descendants('assignment_expression').any { a -> a.field('left').text ==~ /(req|request|ctx|res\.locals|session|context)\..*/ && a.field('right').mentions(v.text) }
                        || scope.descendants('if_statement').any { i -> i.field('condition').mentions(v.text) }
                        || scope.descendants('return_statement').any { r -> r.mentions(v.text) })))
    report c, "jwt.verify() is told to accept expired tokens (ignoreExpiration: true): a stolen token works forever",
        fn ==~ /(jwt|jsonwebtoken)\.verify/ && hasIgnoreExpiration(opts)
    report c, "jwt.verify() accepts the 'none' algorithm, which means no signature at all: pin algorithms: ['RS256']",
        fn ==~ /(jwt|jsonwebtoken)\.verify/ && algorithmsNone(opts)
    // "Do not flag": verify with pinned algorithms.
    report c, "jwt.verify() does not pin the algorithms, so the token's own header chooses how it is checked: pass { algorithms: ['RS256'] }",
        fn ==~ /(jwt|jsonwebtoken)\.verify/ && !opaque(opts) && !hasAlgorithms(opts)
    // "Do not flag": a jose verification that sets issuer or audience, and a file that never reads iss or aud.
    report c, "jwtVerify() sets no issuer or audience, so a token meant for another service is accepted, although this file reads iss/aud claims: pass { issuer, audience }",
        fn in ['jose.jwtVerify', 'jwtVerify'] && !opaque(opts) && !hasIssuerOrAudience(opts) && (module.mentions('iss') || module.mentions('aud'))
}
