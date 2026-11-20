// generated-from: review/common/hardcoded-credential/rule.md
// rule-hash: e086499f9a4c73dfc02de34a73606ee4f89f60273db9636645e7280238657eb6
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'hardcoded-credential'

def secretName = { n -> n ==~ /(?i).*(password|passwd|pwd|secret|token|api[-_]?key|private[-_]?key).*/ }

// The message never contains the value.
Text.each { t ->
    // "Do not flag": obviously fake or placeholder values. A key name such as "password" or "api-key" is not a secret either.
    skip t.text ==~ /(?is).*(changeme|xxx|<[^>]*>|\$\{|example|dummy|fake|sample|your[-_ ]).*/
    skip file.isTest && t.text ==~ /(?is).*\btest\w*\b.*/

    report t, "A secret in the source is visible to everyone with access to the repository and stays in its history: read it from the environment or a secret store",
        t.text ==~ /(?s).*(AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{36,}|xox[baprs]-|-----BEGIN (RSA |EC )?PRIVATE KEY-----|eyJ[A-Za-z0-9_-]{10,}\.eyJ).*/
    report t, "A URL carries a user name and password in the source: read the credentials from the environment or a secret store",
        t.text ==~ /(?s).*:\/\/[^\/\s:@"]+:[^\/\s@"]+@.*/
    // A secret-named variable or field with a literal of 8 or more characters that is not itself a name.
    report t, "The secret ${t.boundName} is a literal in the source: read it from the environment or a secret store",
        secretName(t.boundName) && t.text ==~ /(?s)"[^"]{8,}"/ && !(t.text ==~ /(?i)"[a-z_.\-]*(password|secret|token|key)[a-z_.\-]*"/)
    // The same in a comment: password = "abcd1234efgh"
    report t, "A comment contains a secret: remove it, and rotate it, since it stays in the history",
        t instanceof Comment && t.text ==~ /(?is).*(password|passwd|pwd|secret|token|api[-_]?key)\s*[:=]\s*["'][^"']{8,}["'].*/
}
