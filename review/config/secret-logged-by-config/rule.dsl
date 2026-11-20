// generated-from: review/config/secret-logged-by-config/rule.md
// rule-hash: e80518219547188e09746ba3143979356dadcf828cd4e138748eb45410326c4d
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'secret-logged-by-config'

def verbose = { v -> v ==~ /(?i)\s*(DEBUG|TRACE)\s*/ }

// Whether the verbose output really carries secrets depends on what the application sends, so this is a candidate for the AI.
Setting.each { s ->
    // "Do not flag": dev, local and test files and profiles, and values that the environment supplies.
    skip file.isTest || file.path ==~ /(?i).*[-.](dev|local|test)\.[a-z]+$/
    skip s.profile ==~ /(?i)(dev|local|test)/
    skip s.value ==~ /(?s).*\$\{.*/

    candidate s, "${s.rawKey} is set to ${s.value}, which logs whole HTTP requests and responses, headers and tokens included: keep it off outside development",
        s.key ==~ /logging\.level\.org\.apache\.(http|hc\.client5\.http)\.wire/ && verbose(s.value)
    candidate s, "${s.rawKey} is set to ${s.value}, which logs request details such as headers and bodies: keep it off outside development",
        s.key ==~ /logging\.level\.org\.springframework\.web\.(client|filter\.commons-request-logging-filter)/ && verbose(s.value)
    candidate s, "${s.rawKey} is true, which logs request parameters and headers: keep it off outside development",
        s.key == 'spring.mvc.log-request-details' && s.value ==~ /(?i)\s*true\s*/
    candidate s, "${s.rawKey} is set to ${s.value}, which logs every bound SQL parameter, passwords and tokens included: keep it off outside development",
        s.key ==~ /logging\.level\.org\.hibernate\.(type\.descriptor\.sql|orm\.jdbc\.bind)/ && s.value ==~ /(?i)\s*TRACE\s*/
    candidate s, "${s.rawKey} is ALWAYS, so the actuator shows the real values of environment variables or configuration, secrets included: use WHEN_AUTHORIZED or NEVER",
        s.key ==~ /management\.endpoint\.(env|configprops)\.show-values/ && s.value ==~ /(?i)\s*ALWAYS\s*/
    candidate s, "${s.rawKey} is true, which logs request or response content, headers and bodies included: keep it off outside development",
        s.key ==~ /(.*\.)?(log-headers|log-body|log-request-body|log-response-body|log-payload|log-requests?)/ && s.value ==~ /(?i)\s*true\s*/
}
