// generated-from: review/java/hardcoded-secret-or-environment-url/rule.md
// rule-hash: 64b4c1f498b924c857cacd362e2bf0fcaf7504dfa6cda2c8032f4326c22c76f6
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'hardcoded-secret-or-environment-url'

def secretName = { n -> n ==~ /(?i).*(password|passwd|secret|token|api[-_]?key|private[-_]?key|credential).*/ }
def placeholder = { v -> v ==~ /(?is).*(changeme|\$\{|<[^>]*>|example|dummy|fake|sample|your[-_ ]|xxx).*/ }
def schema = { v -> v ==~ /(?is)"https?:\/\/(www\.w3\.org|schemas?\.|json-schema\.org|java\.sun\.com|maven\.apache\.org|www\.springframework\.org|xmlns).*/ }

// Whether a value is real, and whether the URL is an environment's, is not certain, so these are candidates for the AI.
StringLit.each { t ->
    skip file.isTest
    // "Do not flag": a @Value placeholder or any annotation value, and configuration classes, where an endpoint belongs.
    skip t.inside(Annotation)
    def configClass = t.enclosingClass != null && t.enclosingClass.text ==~ /(?s)\s*(@\w+(\([^)]*\))?\s*)*@(Configuration|ConfigurationProperties)\b.*/

    candidate t, "${t.boundName} is a secret written in the source: read it from the environment or a secret store",
        secretName(t.boundName) && t.text ==~ /"[^"]{6,}"/ && !placeholder(t.text) && !(t.text ==~ /(?i)"[a-z_.\-]*(password|secret|token|key)[a-z_.\-]*"/)
    candidate t, "A secret in a known format is written in the source: remove it and rotate it",
        t.text ==~ /(?s).*(AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----|Bearer [A-Za-z0-9._\-]{16,}).*/ && !placeholder(t.text)
    candidate t, "A URL for a particular environment is written in the source: read it from configuration",
        !configClass && !schema(t.text) && t.text ==~ /(?is)"https?:\/\/(localhost|127\.0\.0\.1|\d{1,3}(\.\d{1,3}){3}|[^\/"]*(dev|staging|stage|test|prod|internal|uat|qa)[^\/"]*)[:\/"].*/
}
