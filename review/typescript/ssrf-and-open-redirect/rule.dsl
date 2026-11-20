// generated-from: review/typescript/ssrf-and-open-redirect/rule.md
// rule-hash: 105cbc0e7f721bb2ab9d251c2faaebcaaf4b0d901aa889da138f3d444a91e536
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'ssrf-and-open-redirect'

def client = { t -> t in ['fetch', 'axios', 'got', 'request', 'needle', 'superagent'] || t ==~ /(axios|got|request|needle|superagent)\.(get|post|put|patch|delete|head|request)/ || t ==~ /https?\.(request|get)/ || t in ['undici.fetch', 'undici.request'] }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    def a0 = c.arg(0)
    only a0 != null && a0.type != 'string'
    def fromRequest = a0.derivedFrom('req', f) || a0.derivedFrom('request', f) || a0.derivedFrom('message', f) || a0.derivedFrom('query', f)
    only fromRequest
    // "Do not flag": a URL whose scheme and host are constant, with request values only after the host (in path segments).
    skip a0.type == 'template_string' && a0.text ==~ /(?s)`https?:\/\/[^\/$`]+\/.*/

    // "Do not flag": a host matched against an allow-list after parsing, and a redirect that can only be a relative path.
    candidate c, "${fn}() fetches a URL that comes from the request: a caller can make the server reach internal services and cloud metadata (169.254.169.254). Parse it with new URL, check the host against an allow-list, and refuse private addresses",
        client(fn) && !(f.text ==~ /(?s).*(allowlist|allowedHosts|ALLOWED|allowed_hosts|isPrivate|ipaddr|isIP|dns\.lookup).*/)
    candidate c, "res.redirect() sends the user to an address from the request: a link on a trusted site can lead to a hostile one (open redirect). Allow only relative paths that start with a single '/'",
        fn ==~ /(\w+\.)?redirect/ && !(f.text ==~ /(?s).*(startsWith\(\s*["']\/["']|allowlist|allowedHosts|ALLOWED|new URL).*/)
}
