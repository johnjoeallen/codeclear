// generated-from: review/typescript/token-in-web-storage/rule.md
// rule-hash: 5a860bcc9746cdec93faf7c33c0e6a26a07a98115b2609a732fe01bdf0e6aeed
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'token-in-web-storage'

def storage = { t -> t in ['localStorage', 'sessionStorage', 'window.localStorage', 'window.sessionStorage'] }
// A sensitive word as a whole word of the key: 'accessToken', 'x-auth-token', 'jwt', but not 'authorName' or 'theme'.
def sensitive = { k -> k ==~ /(?s)["'`]?(.*[^A-Za-z])?(token|jwt|secret|password|credential|auth|session|api[-_]?key)s?([^a-z].*)?["'`]?/
    || k ==~ /(?s)["'`]?.*(Token|Jwt|Secret|Password|Credential|Auth|Session|ApiKey)s?([^a-z].*)?["'`]?/
    || k ==~ /(?s)["'`]?(.*[^A-Z])?(TOKEN|JWT|SECRET|PASSWORD|CREDENTIAL|AUTH|SESSION|API[-_]?KEY)S?([^A-Z].*)?["'`]?/ }

Node.each { n ->
    only n.type in ['call_expression', 'assignment_expression']
    skip file.isTest

    report n, "A token or credential is saved in web storage, where any script on the page can read it: keep it in an HttpOnly cookie",
        n.type == 'call_expression' && n.field('function').type == 'member_expression' && n.field('function').field('property').text == 'setItem'
            && storage(n.field('function').field('object').text) && n.arg(0) != null && sensitive(n.arg(0).text)
    report n, "A token or credential is saved in web storage, where any script on the page can read it: keep it in an HttpOnly cookie",
        n.type == 'assignment_expression' && n.field('left').type == 'member_expression' && storage(n.field('left').field('object').text)
            && sensitive(n.field('left').field('property').text)
    report n, "A token or credential is saved in web storage, where any script on the page can read it: keep it in an HttpOnly cookie",
        n.type == 'assignment_expression' && n.field('left').type == 'subscript_expression' && storage(n.field('left').field('object').text)
            && sensitive(n.field('left').field('index').text)
}
