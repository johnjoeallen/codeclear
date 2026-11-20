// generated-from: review/typescript/cookie-cors-csrf-misconfig/rule.md
// rule-hash: cfe9f70ce0e2e3ebb4cd0625ebc28564e9090bcbe637bfd03d9aeb5096cfbf1b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'cookie-cors-csrf-misconfig'

def sessionName = { n -> n ==~ /(?i)["'`]?.*(session|sid|auth|token|jwt|login|remember).*["'`]?/ }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    def module = c.ancestor('program')
    def o = c.arg(2)
    // "Do not flag": development-only configuration.
    skip c.ancestors('if_statement').any { i -> i.field('condition').text ==~ /(?s).*NODE_ENV\s*===?\s*["'](development|dev|test)["'].*/ }

    candidate c, "This session or auth cookie is set without httpOnly, secure and sameSite: scripts can read it, it travels over plain HTTP, and other sites can send it. Set { httpOnly: true, secure: true, sameSite: 'lax' }",
        fn ==~ /(\w+\.)?cookie/ && c.arg(0) != null && sessionName(c.arg(0).text) && (o == null || o.type != 'object'
            || !(o.text ==~ /(?s).*httpOnly\s*:\s*true.*/) || !(o.text ==~ /(?s).*secure\s*:\s*(true|process|isProd|!).*/) || !(o.text ==~ /(?s).*sameSite.*/))
    candidate c, "The session cookie options turn off httpOnly or secure, or set no sameSite: scripts can read the cookie, or it travels over plain HTTP, or other sites can send it",
        fn in ['session', 'cookieSession', 'expressSession'] && c.arg(0) != null && c.arg(0).type == 'object'
            && (c.arg(0).text ==~ /(?s).*(httpOnly\s*:\s*false|secure\s*:\s*false).*/ || !(c.arg(0).text ==~ /(?s).*sameSite.*/))
    // CORS that lets in every origin together with credentials.
    candidate c, "cors() reflects every origin (origin: true), so with credentials any site can call this API as the signed-in user: list the origins that are allowed",
        fn == 'cors' && c.arg(0) != null && c.arg(0).type == 'object' && (c.arg(0).text ==~ /(?s).*origin\s*:\s*true.*/ || c.arg(0).text ==~ /(?s).*origin\s*:\s*(req|request)\.headers\.origin.*/)
    candidate c, "cors() allows every origin ('*') together with credentials: any site can call this API as the signed-in user",
        fn == 'cors' && c.arg(0) != null && c.arg(0).type == 'object' && c.arg(0).text ==~ /(?s).*origin\s*:\s*["']\*["'].*/ && c.arg(0).text ==~ /(?s).*credentials\s*:\s*true.*/
    candidate c, "'trust proxy' is true, so any client can set X-Forwarded-For and fake its address: name the proxy (a count, or its address)",
        fn ==~ /(\w+\.)?set/ && c.arg(0) != null && c.arg(0).text ==~ /["']trust proxy["']/ && c.arg(1) != null && c.arg(1).type == 'true'
    // A state-changing route in a file that uses cookie sessions and has no CSRF middleware. "Do not flag": a bearer-token API.
    candidate c, "This route changes state in a file that uses cookie sessions and no CSRF protection: another site can make the browser send the request with the cookie. Add csrf middleware, or use sameSite: 'strict'",
        fn ==~ /(app|router)\.(post|put|patch|delete)/ && module.text ==~ /(?s).*(express-session|cookie-session|req\.session).*/ && !(module.text ==~ /(?is).*(csrf|csurf|lusca|sameSite\s*:\s*["']strict|bearer|Authorization).*/)
}
