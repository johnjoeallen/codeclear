// generated-from: review/config/secret-in-config/rule.md
// rule-hash: dc635262ff21a4dbb861ae55bdd35d02f4805ea45ae83c6b75e0de05a20c46d2
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'secret-in-config'

def secretKey = { k -> k ==~ /(?i).*(password|passwd|secret|token|api[-_.]?key|private[-_.]?key|client[-_.]?secret|credentials?).*/ }
// A key that names where the secret is, not the secret: passwordFile, secretName, tokenUrl.
def locationKey = { k -> k ==~ /(?i).*(file|name|url|uri|path|location|ref|arn|id|header|key-name|enabled|expiry|ttl|length)$/ }

// The finding names the key and never contains the value.
Setting.each { s ->
    only !s.value.isEmpty()
    skip file.isTest
    // "Do not flag": placeholders and encrypted or vault references.
    skip s.value ==~ /(?is)\s*(\$\{.*|\{\{.*|<.*>|ENC\(.*|vault:.*|secretsmanager:.*|changeme|x{3,}|\*+)\s*/
    skip s.value ==~ /(?is).*(changeme|\$\{).*/

    report s, "${s.rawKey} holds a credential that matches a known token format: it stays in the repository history, so remove it and rotate it",
        s.value ==~ /(?s).*(AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{36,}|-----BEGIN .* PRIVATE KEY-----).*/
    report s, "${s.rawKey} carries a user name and password in the URL: read them from the environment or a secret store",
        s.value ==~ /(?s).*:\/\/[^\/\s:@]+:[^\/\s@]+@.*/
    report s, "${s.rawKey} is a literal secret in a file under version control: read it from the environment or a secret store",
        secretKey(s.key) && !locationKey(s.key)
}
