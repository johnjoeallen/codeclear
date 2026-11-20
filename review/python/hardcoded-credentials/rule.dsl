// generated-from: review/python/hardcoded-credentials/rule.md
// rule-hash: 18170985c8c3b176e8eb8795b2dc15a150f474d14fd54537c7842df76abab9ff
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'hardcoded-credentials'

def secretName = { n -> n ==~ /(?i)(\w*[_.-])?\w*(password|passwd|secret|token|api[_-]?key|private[_-]?key|client[_-]?secret|aws_secret_access_key)\w*/ }
// A name that says what or where the secret is, not the secret: token_count, password_file, secret_name, api_key_url.
def labelName = { n -> n ==~ /(?i).*(count|length|len|size|type|url|uri|file|path|name|id|header|field|label|prefix|ttl|expiry|expires|endpoint)$/ }
def placeholder = { v -> v ==~ /(?is).*(changeme|<[^>]*>|\*{3,}|xxx|example|dummy|fake|sample|your[_ -]|placeholder).*/ }
def known = { v -> v ==~ /(?s).*(AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----|xox[bp]-|ghp_[A-Za-z0-9]{36}).*/ }
def literal = { v -> v != null && v.type == 'string' && v.descendants('interpolation').isEmpty() && v.text ==~ /(?s)[a-zA-Z]{0,2}("""|'''|"|').{8,}("""|'''|"|')/ }

// Whether the value is a real secret is for the AI to judge, so these are candidates.
Node.each { n ->
    only n.type in ['string', 'assignment', 'keyword_argument', 'default_parameter']
    skip file.isTest
    def name = n.type == 'assignment' ? n.field('left') : n.field('name')
    def value = n.type == 'assignment' ? n.field('right') : n.field('value')
    // The name, for an attribute target such as self.password.
    def label = name == null ? '' : (name.type == 'attribute' ? name.field('attribute').text : name.text)

    candidate n, "A secret in a known format is written in the source: it stays in the repository history, so remove it and rotate it",
        n.type == 'string' && known(n.text) && !placeholder(n.text)
    candidate n, "A connection string carries a user name and password in the source: read them from the environment or a secret store",
        n.type == 'string' && n.text ==~ /(?s).*[a-z][a-z0-9+.\-]*:\/\/[^\/\s:@"']+:[^\/\s@"']+@.*/ && !placeholder(n.text)
    // "Do not flag": placeholders, label names and dictionary keys (a pair is not looked at), and anything read from the environment.
    candidate n, "${label} is a secret written in the source: read it from the environment or a secret store",
        value != null && literal(value) && secretName(label) && !labelName(label) && !placeholder(value.text) && !known(value.text)
}
