// generated-from: review/python/token-verification-and-session-config/rule.md
// rule-hash: 3f7f9842c0b7a1c36150e186bd671a90a100b2f5c46e2c11616d4a9453ce0baf
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'token-verification-and-session-config'

Node.each { n ->
    only n.type in ['call', 'assignment', 'decorator', 'string']
    skip file.isTest
    def fn = n.type == 'call' ? n.field('function').text : ''
    def left = n.type == 'assignment' ? n.field('left').text : ''
    def right = n.type == 'assignment' ? n.field('right') : n

    candidate n, "jwt.decode() is told not to verify the signature, so anyone can forge a token: remove verify_signature=False",
        fn in ['jwt.decode', 'decode'] && n.keyword('options') != null && n.keyword('options').text ==~ /(?s).*verify_signature["']\s*:\s*False.*/
    candidate n, "${fn}() is called with verify=False, so the signature or certificate is not checked",
        fn ==~ /(jwt\.)?(decode|verify)/ && n.keyword('verify') != null && n.keyword('verify').type == 'false'
    candidate n, "jwt.decode() without algorithms= lets the token pick its own algorithm, including 'none': pass algorithms=['RS256']",
        fn == 'jwt.decode' && n.keyword('algorithms') == null && n.keyword('options') == null
    candidate n, "algorithms includes 'none', which means no signature at all",
        fn == 'jwt.decode' && n.keyword('algorithms') != null && n.keyword('algorithms').text ==~ /(?is).*["']none["'].*/
    // A cookie without the flags that keep it from scripts and from other sites.
    candidate n, "set_cookie() without secure, httponly and samesite: the cookie travels over plain HTTP, is readable by scripts and is sent by other sites",
        fn ==~ /(\w+\.)*set_cookie/ && (n.keyword('secure') == null || n.keyword('httponly') == null || n.keyword('samesite') == null)
    candidate n, "${left} is turned off: the session cookie loses a protection",
        left ==~ /SESSION_COOKIE_(SECURE|HTTPONLY)|CSRF_COOKIE_(SECURE|HTTPONLY)/ && right.type == 'false'
    candidate n, "@csrf_exempt turns off CSRF protection for this view: with cookie-based sessions another site can make the browser post to it",
        n.type == 'decorator' && n.text ==~ /(?s)\s*@\s*(\w+\.)*csrf_exempt\s*/
    // CORS that allows every origin together with credentials.
    candidate n, "CORS allows every origin and credentials: any site can make requests as the signed-in user",
        fn ==~ /(\w+\.)*(CORSMiddleware|add_middleware|CORS)/ && n.keyword('allow_origins') != null && n.keyword('allow_origins').text ==~ /(?s).*["']\*["'].*/
            && n.keyword('allow_credentials') != null && n.keyword('allow_credentials').type == 'true'
    candidate n, "CORS_ALLOW_ALL_ORIGINS is on: any site can call this API from a browser",
        left == 'CORS_ALLOW_ALL_ORIGINS' && right.type == 'true'
    // X-Forwarded-* trusted without saying which proxy sets it.
    candidate n, "X-Forwarded-* headers are read without a trusted-proxy setting: a client can send its own and fake its address or scheme",
        n.type == 'string' && n.text ==~ /(?is)["']x-forwarded-(for|proto|host)["']/ && !(n.ancestor('module').text ==~ /(?is).*(trusted_proxies|trusted_proxy|proxy_headers|forwarded_allow_ips|ProxyFix|set_real_ip|TRUSTED_PROXY).*/)
}
