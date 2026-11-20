// generated-from: review/config/insecure-default/rule.md
// rule-hash: 4bf9633c52a1114b4166b67836031215040cec2046556e8760feea1c3909d4ef
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'insecure-default'

def on = { v -> v ==~ /(?i)\s*true\s*/ }
def off = { v -> v ==~ /(?i)\s*false\s*/ }

Setting.each { s ->
    // "Do not flag": files and profiles marked local, dev or test, and values supplied from the environment.
    skip file.isTest || file.path ==~ /(?i).*[-.](dev|local|test)\.[a-z]+$/
    skip s.profile ==~ /(?i)(dev|local|test)/
    skip s.value ==~ /(?s).*\$\{.*/

    report s, "${s.rawKey} exposes every actuator endpoint: list only the ones that are needed",
        s.key ==~ /management\.endpoints?\.web\.exposure\.include.*/ && s.value ==~ /.*(^|,)\s*\*\s*(,|$).*/
    report s, "${s.rawKey} shows health details to everyone: use when-authorized",
        s.key == 'management.endpoint.health.show-details' && s.value ==~ /(?i)\s*always\s*/
    report s, "${s.rawKey} turns TLS off",
        s.key == 'server.ssl.enabled' && off(s.value)
    report s, "${s.rawKey} turns certificate verification off: the connection can be intercepted",
        (s.key ==~ /(.*\.)?ssl\.verify/ && off(s.value)) || (s.key ==~ /(.*\.)?tls\.insecure-skip-verify/ && on(s.value))
            || (s.key ==~ /(.*\.)?trust-all-certificates/ && on(s.value))
    report s, "${s.rawKey} turns authentication or CSRF protection off",
        s.key ==~ /(spring\.security\..*|(.*\.)?auth|(.*\.)?csrf)\.enabled/ && off(s.value)
    report s, "${s.rawKey} turns logging off: failures leave no trace",
        s.key == 'logging.level.root' && s.value ==~ /(?i)\s*off\s*/
    report s, "${s.rawKey} enables the H2 console, which gives database access from the browser",
        s.key == 'spring.h2.console.enabled' && on(s.value)
    report s, "${s.rawKey} lets Hibernate change the schema at start-up: use a migration tool",
        s.key == 'spring.jpa.hibernate.ddl-auto' && s.value ==~ /(?i)\s*(create|create-drop|update)\s*/
    report s, "${s.rawKey} allows every origin while credentials are allowed: any site can act as the signed-in user",
        s.key ==~ /(.*\.)?cors\.allowed-origins(\[\d+\])?/ && s.value ==~ /\s*\*\s*/ && on(s.siblingValue('allow-credentials'))
}
